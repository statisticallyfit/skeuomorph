/*
* Copyright 2018-2023 47 Degrees Open Source <https://www.47deg.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package higherkindness.skeuomorph.openapi

import schema._
import io.circe._
import cats.syntax.all._
import higherkindness.droste._
import higherkindness.droste.syntax.embed._
import io.circe.Decoder.Result

import scala.language.postfixOps

object JsonDecoders {

	implicit val referenceDecoder: Decoder[Reference] = Decoder.forProduct1(s"$$ref")(Reference.apply)

	implicit def orReferenceDecoder[A: Decoder]: Decoder[Either[A, Reference]] =
		Decoder[Reference].map(_.asRight[A]) orElse Decoder[A].map(_.asLeft[Reference])

	private def basicJsonSchemaDecoder[A: Embed[JsonSchemaF, *]]: Decoder[A] = {
		import JsonSchemaF._
		Decoder.forProduct2[(String, Option[String]), String, Option[String]]("type", "format")(Tuple2.apply).emap {
			case ("integer", Some("int32")) => integer[A]().embed.asRight
			case ("integer", Some("int64")) => long[A]().embed.asRight
			case ("integer", _) => integer[A]().embed.asRight
			case ("number", Some("float")) => float[A]().embed.asRight
			case ("number", Some("double")) => double[A]().embed.asRight
			case ("number", _) => float[A]().embed.asRight
			case ("string", Some("byte")) => byte[A]().embed.asRight
			case ("string", Some("binary")) => binary[A]().embed.asRight
			case ("boolean", _) => boolean[A]().embed.asRight
			case ("string", Some("date")) => date[A]().embed.asRight
			case ("string", Some("date-time")) => dateTime[A]().embed.asRight
			case ("string", Some("password")) => password[A]().embed.asRight
			case ("string", _) => string[A]().embed.asRight
			case (x, _) => s"$x is not well formed type".asLeft
		}
	}

	private def validateType(c: HCursor, expected: String): Decoder.Result[Unit] =
		c.downField("type").as[String].flatMap {
			case `expected` => ().asRight
			case actual => DecodingFailure(s"$actual is not expected type $expected", c.history).asLeft
		}

	private def enumJsonSchemaDecoder[A: Embed[JsonSchemaF, *]]: Decoder[A] =
		Decoder.instance(c =>
			for {
				values <- c.downField("enum").as[List[String]]
				_ <- validateType(c, "string")
			} yield JsonSchemaF.enum[A](values).embed
		)

	private def sumJsonSchemaDecoder[A: Embed[JsonSchemaF, *]]: Decoder[A] =
		Decoder.instance(_.downField("oneOf").as[List[A]].map(JsonSchemaF.sum[A](_).embed))

	private def objectJsonSchemaDecoder[A: Embed[JsonSchemaF, *]]: Decoder[A] =

		Decoder.instance { c: HCursor =>

			def propertyExists(name: String): Decoder.Result[Unit] =
				c.downField(name)
					.success
					.fold(DecodingFailure(s"$name property does not exist", c.history).asLeft[Unit])(_ =>
						().asRight[DecodingFailure]
					)

			def isObject: Decoder.Result[Unit] =
				validateType(c, "object") orElse
					propertyExists("properties") orElse
					propertyExists("allOf")

			// Added by @statisticallyfit
			def isObjectMap: Boolean = isObjectNamed && propertyExists("additionalProperties").isRight

			// Added by @statisticallyfit
			def isObjectNamed: Boolean = isObject.isRight && propertyExists("title").isRight

			// Added by @statisticallyfit:
			// Separating by different kinds of object classes so can properly interpret a MAP from avro into json
			def makeJsonCirceObjectMap: Result[A] = {

				val objmap: (String, JsonSchemaF.AdditionalProperties[A]) => JsonSchemaF[A] = (title, addProps) => JsonSchemaF.objectNamedMap[A](name = title, additionalProperties = addProps)


				val resultArgs: Result[(String, JsonSchemaF.AdditionalProperties[A])] = for {

					title: String <- c.downField("title").as[Option[String]].map(_.getOrElse(""))
					addProps: JsonSchemaF.AdditionalProperties[A] <- {
						//.as[Option[A]]

						val resOptMap: Result[Option[Map[String, A]]] = c.downField("additionalProperties").as[Option[Map[String, A]]](
							Decoder.decodeOption(Decoder.decodeMap[String, A](KeyDecoder.decodeKeyString, jsonSchemaDecoder[A]))
						)

						val resMap: Result[Map[String, A]] = resOptMap.map(_.getOrElse(Map.empty))

						val resAddedProps: Result[JsonSchemaF.AdditionalProperties[A]] = {
							resMap.map(_.toList.map(tup => JsonSchemaF.AdditionalProperties.apply[A](tup._2)).head)
							//resMap.map((mapStrA: Map[String, A]) => mapStrA.toList.map((tup: (String, A)) => JsonSchemaF.AdditionalProperties(tup._2) ).head )

						}
						println(s"\n\nINSIDE makeJsonCirceObjectMap:")
						println(s"resOptMap  = $resOptMap")
						println(s"resMap  = $resMap")
						println(s"resAddedProps = $resAddedProps")

						resAddedProps
					}
				} yield /*objmap*/ (title, addProps) //JsonSchemaF.objectMap[A](name = title, additionalProperties = addProps).embed


				val result_noEmbed: Result[JsonSchemaF[A]] = resultArgs.map { case (title, addProps) => objmap(title, addProps) }
				val result_embed: Result[A] = resultArgs.map { case (title, addProps) => objmap(title, addProps).embed }

				println(s"\n\nINSIDE makeJsonCirceObjectMap:")
				println(s"result (not embed) = $result_noEmbed")
				println(s"result (embed) = $result_embed")

				result_embed
			}


			def makeJsonCirceObjectNamed: Result[A] = {

				val objname: (String, List[JsonSchemaF.Property[A]], List[String]) => JsonSchemaF[A] = (title, properties, required) => JsonSchemaF.objectNamed[A](name = title, properties = properties, required = required)

				val resultArgs: Result[(String, List[JsonSchemaF.Property[A]], List[String])] =
					for {
						title: String <- c.downField("title").as[Option[String]].map(_.getOrElse(""))
						required: List[String] <- c.downField("required").as[Option[List[String]]].map(_.getOrElse(List.empty))
						properties: List[JsonSchemaF.Property[A]] <- {
							//.as[Option[A]]
							val resOptMap: Result[Option[Map[String, A]]] = c.downField("properties").as[Option[Map[String, A]]](
								Decoder.decodeOption(Decoder.decodeMap[String, A](KeyDecoder.decodeKeyString, jsonSchemaDecoder[A]))
							)

							val resMap: Result[Map[String, A]] = resOptMap.map(_.getOrElse(Map.empty))

							val resListProps: Result[List[JsonSchemaF.Property[A]]] = resMap
								.map((mapStrA: Map[String, A]) => mapStrA.toList.map((JsonSchemaF.Property.apply[A] _).tupled /*{
								val funcTup: ((String, A)) => JsonSchemaF.Property[A] = (JsonSchemaF.Property.apply[A] _).tupled
								funcTup(tup)
							}*/))


							println(s"\n\nINSIDE makeJsonCirceObjectNamed:")
							println(s"resOptMap  = $resOptMap")
							println(s"resMap  = $resMap")
							println(s"resListProps = $resListProps")

							resListProps
						}
					} yield (title, properties, required) //JsonSchemaF.objectName[A](title, properties, required).embed

				val result_noEmbed: Result[JsonSchemaF[A]] = resultArgs.map { case (title, ps, rs) => objname(title, ps, rs) }
				val result_embed: Result[A] = resultArgs.map { case (title, ps, rs) => objname(title, ps, rs).embed }

				println(s"\n\nINSIDE makeJsonCirceObjectNamed:")
				println(s"result (not embed) = $result_noEmbed")
				println(s"result (embed) = $result_embed")

				result_embed

			}



			def makeJsonCirceObjectSimple = {

				val objsimple: (List[JsonSchemaF.Property[A]], List[String]) => JsonSchemaF[A] = (properties, required) => JsonSchemaF.`object`[A](properties, required)


				val resultArgs: Result[(List[JsonSchemaF.Property[A]], List[String])] =
					for {
						//itle: String <- c.downField("title").as[Option[String]].map(_.getOrElse(""))
						required: List[String] <- c.downField("required").as[Option[List[String]]].map(_.getOrElse(List.empty))
						properties: List[JsonSchemaF.Property[A]] <- {
							//.as[Option[A]]
							val resOptMap: Result[Option[Map[String, A]]] = c.downField("properties").as[Option[Map[String, A]]](
								Decoder.decodeOption(Decoder.decodeMap[String, A](KeyDecoder.decodeKeyString, jsonSchemaDecoder[A]))
							)

							val resMap: Result[Map[String, A]] = resOptMap.map(_.getOrElse(Map.empty))

							val resListProps: Result[List[JsonSchemaF.Property[A]]] = resMap
								.map((mapStrA: Map[String, A]) => mapStrA.toList.map((JsonSchemaF.Property.apply[A] _).tupled /*{
								val funcTup: ((String, A)) => JsonSchemaF.Property[A] = (JsonSchemaF.Property.apply[A] _).tupled
								funcTup(tup)
							}*/))


							println(s"\n\nINSIDE makeJsonCirceObjectNamed:")
							println(s"resOptMap  = $resOptMap")
							println(s"resMap  = $resMap")
							println(s"resListProps = $resListProps")

							resListProps
						}
					} yield ( properties, required) // JsonSchemaF.`object`[A](properties, required.getOrElse(List.empty)).embed

				val result_noEmbed: Result[JsonSchemaF[A]] = resultArgs.map { case (ps, rs) => objsimple(ps, rs) }
				val result_embed: Result[A] = resultArgs.map { case (ps, rs) => objsimple(ps, rs).embed }

				println(s"\n\nINSIDE makeJsonCirceObjectSimple:")
				println(s"result (not embed) = $result_noEmbed")
				println(s"result (embed) = $result_embed")

				result_embed
			}


			// Case matching to decide which object-type to use
			isObjectMap match {

				case true => makeJsonCirceObjectMap

				case false => isObjectNamed match {

					case true => makeJsonCirceObjectNamed

					case false => makeJsonCirceObjectSimple
				}
			}


		}

	private def arrayJsonSchemaDecoder[A: Embed[JsonSchemaF, *]]: Decoder[A] =
		Decoder.instance { c =>
			for {
				items <- c.downField("items").as[A](jsonSchemaDecoder[A])
				_ <- validateType(c, "array")
			} yield JsonSchemaF.array(items).embed
		}

	private def referenceJsonSchemaDecoder[A: Embed[JsonSchemaF, *]]: Decoder[A] =
		Decoder[Reference].map(x => JsonSchemaF.reference[A](x.ref).embed)

	implicit def jsonSchemaDecoder[A: Embed[JsonSchemaF, *]]: Decoder[A] =
		referenceJsonSchemaDecoder orElse
			sumJsonSchemaDecoder orElse
			objectJsonSchemaDecoder orElse
			arrayJsonSchemaDecoder orElse
			enumJsonSchemaDecoder orElse
			basicJsonSchemaDecoder

	implicit val infoDecoder: Decoder[Info] =
		Decoder.forProduct3(
			"title",
			"description",
			"version"
		)(Info.apply)

	implicit val serverVariableDecoder: Decoder[Server.Variable] =
		Decoder.forProduct3(
			"enum",
			"default",
			"description"
		)((enum: Option[List[String]], default: String, description: Option[String]) =>
			Server.Variable(enum.getOrElse(List.empty), default, description)
		)

	implicit val serverDecoder: Decoder[Server] =
		Decoder.forProduct3(
			"url",
			"description",
			"variables"
		) { (url: String, description: Option[String], variables: Option[Map[String, Server.Variable]]) =>
			Server(url, description, variables.getOrElse(Map.empty))
		}

	implicit val externalDocsDecoder: Decoder[ExternalDocs] =
		Decoder.forProduct2(
			"url",
			"description"
		)(ExternalDocs.apply)

	implicit val tagDecoder: Decoder[Tag] =
		Decoder.forProduct3(
			"name",
			"description",
			"externalDocs"
		)(Tag.apply)

	implicit def headerDecoder[A: Decoder]: Decoder[Header[A]] =
		Decoder.forProduct2(
			"description",
			"schema"
		)(Header.apply)

	implicit def encodingDecoder[A: Decoder]: Decoder[Encoding[A]] =
		Decoder.forProduct5(
			"contentType",
			"headers",
			"style",
			"explode",
			"allowReserved"
		)(
			(
				contentType: Option[String],
				headers: Option[Map[String, Either[Header[A], Reference]]],
				style: Option[String],
				explode: Option[Boolean],
				allowReserved: Option[Boolean]
			) => Encoding(contentType, headers.getOrElse(Map.empty), style, explode, allowReserved)
		)

	implicit def mediaTypeDecoder[A: Decoder]: Decoder[MediaType[A]] =
		Decoder.forProduct2(
			"schema",
			"encoding"
		)((schema: Option[A], encoding: Option[Map[String, Encoding[A]]]) =>
			MediaType(schema, encoding.getOrElse(Map.empty))
		)

	implicit def requestDecoder[A: Decoder]: Decoder[Request[A]] =
		Decoder.forProduct3(
			"description",
			"content",
			"required"
		)((description: Option[String], content: Map[String, MediaType[A]], required: Option[Boolean]) =>
			Request(description, content, required.getOrElse(false))
		)

	implicit def responseDecoder[A: Decoder]: Decoder[Response[A]] =
		Decoder.forProduct3(
			"description",
			"headers",
			"content"
		)(
			(
				description: String,
				headers: Option[Map[String, Either[Header[A], Reference]]],
				content: Option[Map[String, MediaType[A]]]
			) => Response(description, headers.getOrElse(Map.empty), content.getOrElse(Map.empty))
		)

	implicit val locationDecoder: Decoder[Location] = Decoder.decodeString.emap(Location.parse)

	implicit def parameterDecoder[A: Decoder]: Decoder[Parameter[A]] =
		Decoder.forProduct10(
			"name",
			"in",
			"description",
			"required",
			"deprecated",
			"style",
			"explode",
			"allowEmptyValue",
			"allowReserved",
			"schema"
		)(Parameter.apply)

	// Avoid forProductXX
	implicit def operationDecoder[A: Decoder]: Decoder[Path.Operation[A]] =
		Decoder.instance(c =>
			for {
				tags <- c.downField("tags").as[Option[List[String]]]
				summary <- c.downField("summary").as[Option[String]]
				description <- c.downField("description").as[Option[String]]
				externalDocs <- c.downField("externalDocs").as[Option[ExternalDocs]]
				operationId <- c.downField("operationId").as[Option[String]]
				parameters <- c.downField("parameters").as[Option[List[Either[Parameter[A], Reference]]]]
				requestBody <- c.downField("requestBody").as[Option[Either[Request[A], Reference]]]
				responses <- c.downField("responses").as[Map[String, Either[Response[A], Reference]]]
				callbacks <- c.downField("callbacks").as[Option[Map[String, Either[Callback[A], Reference]]]]
				deprecated <- c.downField("deprecated").as[Option[Boolean]]
				servers <- c.downField("servers").as[Option[List[Server]]]
			} yield Path.Operation(
				tags.getOrElse(List.empty),
				summary,
				description,
				externalDocs,
				operationId,
				parameters.getOrElse(List.empty),
				requestBody,
				responses,
				callbacks.getOrElse(Map.empty),
				deprecated.getOrElse(false),
				servers.getOrElse(List.empty)
			)
		)

	implicit def itemObjectDecoder[A: Decoder]: Decoder[Path.ItemObject[A]] =
		Decoder.forProduct13(
			"ref",
			"summary",
			"description",
			"get",
			"put",
			"post",
			"delete",
			"options",
			"head",
			"patch",
			"trace",
			"servers",
			"parameters"
		)(
			(
				ref: Option[String], // $ref
				summary: Option[String],
				description: Option[String],
				get: Option[Path.Operation[A]],
				put: Option[Path.Operation[A]],
				post: Option[Path.Operation[A]],
				delete: Option[Path.Operation[A]],
				options: Option[Path.Operation[A]],
				head: Option[Path.Operation[A]],
				patch: Option[Path.Operation[A]],
				trace: Option[Path.Operation[A]],
				servers: Option[List[Server]],
				parameters: Option[List[Either[Parameter[A], Reference]]]
			) =>
				Path.ItemObject(
					ref,
					summary,
					description,
					get,
					put,
					post,
					delete,
					options,
					head,
					patch,
					trace,
					servers.getOrElse(List.empty),
					parameters.getOrElse(List.empty)
				)
		)

	implicit def componentsDecoder[A: Decoder]: Decoder[Components[A]] =
		Decoder.forProduct4(
			"schemas",
			"responses",
			"requestBodies",
			"parameters"
		)(
			(
				schemas: Option[Map[String, A]],
				responses: Option[Map[String, Either[Response[A], Reference]]],
				requestBodies: Option[Map[String, Either[Request[A], Reference]]],
				parameters: Option[Map[String, Either[Parameter[A], Reference]]]
			) =>
				Components(
					schemas.getOrElse(Map.empty),
					responses.getOrElse(Map.empty),
					requestBodies.getOrElse(Map.empty),
					parameters.getOrElse(Map.empty)
				)
		)

	implicit def openApiDecoder[A: Decoder]: Decoder[OpenApi[A]] =
		Decoder.forProduct7(
			"openapi",
			"info",
			"servers",
			"paths",
			"components",
			"tags",
			"externalDocs"
		)(
			(
				openapi: String,
				info: Info,
				servers: Option[List[Server]],
				paths: Option[Map[String, Path.ItemObject[A]]],
				components: Option[Components[A]],
				tags: Option[List[Tag]],
				externalDocs: Option[ExternalDocs]
			) =>
				OpenApi(
					openapi,
					info,
					servers.getOrElse(List.empty),
					paths.getOrElse(Map.empty),
					components,
					tags.getOrElse(List.empty),
					externalDocs
				)
		)

}
