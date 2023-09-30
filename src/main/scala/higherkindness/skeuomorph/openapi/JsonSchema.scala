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

import io.circe.Json
import higherkindness.droste._
import cats.syntax.all._
import cats._

import higherkindness.droste.data.Fix
import higherkindness.droste.macros.deriveTraverse

// NOTE: changed - removing this 'sealed' so I can create an OjbectF with a name! (like graphql and autoschema do)
@deriveTraverse sealed trait JsonSchemaF[A]
object JsonSchemaF {
  @deriveTraverse final case class Property[A](name: String, tpe: A)
  @deriveTraverse case class AdditionalProperties[A](tpe: A)

  final case class IntegerF[A]()                                                     extends JsonSchemaF[A]
  final case class LongF[A]()                                                        extends JsonSchemaF[A]
  final case class FloatF[A]()                                                       extends JsonSchemaF[A]
  final case class DoubleF[A]()                                                      extends JsonSchemaF[A]
  final case class StringF[A]()                                                      extends JsonSchemaF[A]
  final case class ByteF[A]()                                                        extends JsonSchemaF[A]
  final case class BinaryF[A]()                                                      extends JsonSchemaF[A]
  final case class BooleanF[A]()                                                     extends JsonSchemaF[A]
  final case class DateF[A]()                                                        extends JsonSchemaF[A]
  final case class DateTimeF[A]()                                                    extends JsonSchemaF[A]
  final case class PasswordF[A]()                                                    extends JsonSchemaF[A]
  final case class ObjectF[A](properties: List[Property[A]], required: List[String]) extends JsonSchemaF[A]
  final case class ObjectNamedF[A](name: String, properties: List[Property[A]], required: List[String]) extends JsonSchemaF[A]
  ///Need non-named map because Avro has no name in th emap (wen converting avro-skeuo -> circe -> skeuo, result doesn't work if no name)
  final case class ObjectMapF[A](additionalProperties: AdditionalProperties[A]) extends JsonSchemaF[A]

  // NOTE: this object is to declare map from avro using this syntax =  https://github.com/orgs/json-schema-org/discussions/207#discussioncomment-3219441
  final case class ObjectNamedMapF[A](name: String, additionalProperties: AdditionalProperties[A]) extends JsonSchemaF[A]
  final case class ArrayF[A](values: A)                                              extends JsonSchemaF[A]
  final case class EnumF[A](cases: List[String], name: Option[String] = None)                                     extends JsonSchemaF[A]
  //final case class EnumNamedF[A](name: )
  final case class SumF[A](cases: List[A])                                           extends JsonSchemaF[A]
  final case class ReferenceF[A](ref: String)                                        extends JsonSchemaF[A]

  def integer[T](): JsonSchemaF[T]  = IntegerF()
  def long[T](): JsonSchemaF[T]     = LongF()
  def float[T](): JsonSchemaF[T]    = FloatF()
  def double[T](): JsonSchemaF[T]   = DoubleF()
  def string[T](): JsonSchemaF[T]   = StringF()
  def byte[T](): JsonSchemaF[T]     = ByteF()
  def binary[T](): JsonSchemaF[T]   = BinaryF()
  def boolean[T](): JsonSchemaF[T]  = BooleanF()
  def date[T](): JsonSchemaF[T]     = DateF()
  def dateTime[T](): JsonSchemaF[T] = DateTimeF()
  def password[T](): JsonSchemaF[T] = PasswordF()
  def `object`[T](properties: List[Property[T]], required: List[String]): JsonSchemaF[T] = {
    ObjectF(properties, required)
  }

  def objectNamed[T](name: String, properties: List[Property[T]], required: List[String]): JsonSchemaF[T] = ObjectNamedF(name, properties, required)
  def objectNamedMap[T](name: String, additionalProperties: AdditionalProperties[T]): JsonSchemaF[T] = ObjectNamedMapF(name, additionalProperties)
  def objectMap[T](additionalProperties: AdditionalProperties[T]): JsonSchemaF[T] = ObjectMapF(additionalProperties)
  def array[T](values: T): JsonSchemaF[T]            = ArrayF(values)
  def `enum`[T](cases: List[String], name: Option[String] = None): JsonSchemaF[T] = EnumF(cases, name)
  def sum[T](cases: List[T]): JsonSchemaF[T]         = SumF(cases)
  def reference[T](ref: String): JsonSchemaF[T]      = ReferenceF[T](ref)

  type Fixed = Fix[JsonSchemaF]

  object Fixed {
    def integer[A](): Fix[JsonSchemaF] = Fix(JsonSchemaF.integer())
    def long[A](): Fix[JsonSchemaF]     = Fix(JsonSchemaF.long())
    def float[A](): Fix[JsonSchemaF]   = Fix(JsonSchemaF.float())
    def double[A](): Fix[JsonSchemaF]   = Fix(JsonSchemaF.double())
    def string[A](): Fix[JsonSchemaF]   = Fix(JsonSchemaF.string())
    def byte[A](): Fix[JsonSchemaF]     = Fix(JsonSchemaF.byte())
    def binary[A](): Fix[JsonSchemaF]   = Fix(JsonSchemaF.binary())
    def boolean[A](): Fix[JsonSchemaF]  = Fix(JsonSchemaF.boolean())
    def date[A](): Fix[JsonSchemaF]     = Fix(JsonSchemaF.date())
    def dateTime[A](): Fix[JsonSchemaF] = Fix(JsonSchemaF.dateTime())
    def password[A](): Fix[JsonSchemaF] = Fix(JsonSchemaF.password())
    def `object`(properties: List[(String, JsonSchemaF.Fixed)], required: List[String]): JsonSchemaF.Fixed =
      Fix(JsonSchemaF.`object`(properties.map((JsonSchemaF.Property.apply[JsonSchemaF.Fixed] _).tupled), required))
    def array(value: JsonSchemaF.Fixed): JsonSchemaF.Fixed        = Fix(JsonSchemaF.array(value))
    def `enum`(value: List[String], name: Option[String] = None): Fix[JsonSchemaF]            = Fix(JsonSchemaF.`enum`(value, name))
    def sum[A](value: List[JsonSchemaF.Fixed]): JsonSchemaF.Fixed = Fix(JsonSchemaF.sum(value))
    def reference(value: String): Fix[JsonSchemaF]               = Fix(JsonSchemaF.reference(value))
  }

  private def jsonType(value: String, attr: (String, Json)*): Json =
    Json.obj((("type" -> Json.fromString(value)) :: attr.toList): _*)

  private def format(value: String): (String, Json) =
    "format" -> Json.fromString(value)

  def render: Algebra[JsonSchemaF, Json] =
    Algebra {
      case IntegerF()  => jsonType("integer", format("int32"))
      case LongF()     => jsonType("integer", format("int64"))
      case FloatF()    => jsonType("number", format("float"))
      case DoubleF()   => jsonType("number", format("double"))
      case StringF()   => jsonType("string")
      case ByteF()     => jsonType("string", format("byte"))
      case BinaryF()   => jsonType("string", format("binary"))
      case BooleanF()  => jsonType("boolean")
      case DateF()     => jsonType("string", format("date"))
      case DateTimeF() => jsonType("string", format("date-time"))
      case PasswordF() => jsonType("string", format("password"))
      case ObjectF(properties, required) =>
        jsonType(
          "object",
          "properties" -> Json.obj(properties.map(prop => prop.name -> prop.tpe): _*),
          "required"   -> Json.fromValues(required.map(Json.fromString))
        )
      case ArrayF(values) =>
        jsonType(
          "array",
          "items" -> values
        )

        // Changed by @statisticallyfit : so that EnumF can have name (doing deepMerge)
      case EnumF(cases: List[String], name: Option[String]) => {
        val base: Json = Json.obj(
          "type" -> Json.fromString("string"),
          "enum" -> Json.fromValues(cases.map(Json.fromString))
        )

        val withName: Json = name.fold(base)(n => base deepMerge Json.obj("name" -> Json.fromString(n)))

        val result: Json = if(name.isDefined) withName else base

        result
      }
        //jsonType("string", "enum" -> Json.fromValues(cases.map(Json.fromString)))
      case SumF(cases) =>
        Json.obj("oneOf" -> Json.arr(cases: _*))
      case ReferenceF(value) =>
        Json.obj(
          s"$$ref" -> Json.fromString(value)
        )

    }

  implicit def eqProperty[T: Eq]: Eq[Property[T]] = Eq.instance((p1, p2) => p1.name === p2.name && p1.tpe === p2.tpe)

  implicit def eqJsonSchemaF[T: Eq]: Eq[JsonSchemaF[T]] =
    Eq.instance {
      case (IntegerF(), IntegerF())           => true
      case (LongF(), LongF())                 => true
      case (FloatF(), FloatF())               => true
      case (DoubleF(), DoubleF())             => true
      case (StringF(), StringF())             => true
      case (ByteF(), ByteF())                 => true
      case (BinaryF(), BinaryF())             => true
      case (BooleanF(), BooleanF())           => true
      case (DateF(), DateF())                 => true
      case (DateTimeF(), DateTimeF())         => true
      case (PasswordF(), PasswordF())         => true
      case (ObjectF(p1, r1), ObjectF(p2, r2)) => p1 === p2 && r1 === r2
      case (ArrayF(v1), ArrayF(v2))           => v1 === v2
      case (EnumF(c1, n1), EnumF(c2, n2))             => n1 == n2 && c1 === c2
      case (SumF(c1), SumF(c2))               => c1 === c2
      case (ReferenceF(r1), ReferenceF(r2))   => r1 === r2
      case _                                  => false
    }

}
