
credentials +=
     Credentials(
          "GitHub Package Registry",
          "https://maven.pkg.github.com",
          "statisticallyfit",
          sys.env.getOrElse("GITHUB_TOKEN", "N/A")
     )
/*credentials += Credentials(
     "GitHub Package Registry",
     "maven.pkg.github.com",
     "<GITHUB_OWNER>",
     System.getenv("GITHUB_TOKEN")
)*/
//credentials +=
//     Credentials(
//          "GitHub Package Registry",
//          "maven.pkg.github.com",
//          "GITHUB_OWNER",
//          "GITHUB_TOKEN")
//credentials +=
//     Credentials(
//          "GitHub Package Registry",
//          "maven.pkg.github.com",
//          "statisticallyfit",
//          "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")
