package restui.specifications

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks

class ValidatorSpec extends AnyFlatSpec with TableDrivenPropertyChecks {
  it should "validate a valid specifications" in {
    val properties = Table(
      ("input", "is valid?"),
      ("""{
|  "swagger": "2.0",
| "info": {
|   "version": "1.0.5",
|    "title": "Swagger Petstore",
|  },
|  "host": "petstore.swagger.io",
|  "basePath": "/v2",
|  "schemes": [
|    "https",
|    "http"
|  ],
|  "paths": {
|    "/path": {
|      "get": {
|        "responses": {
|          "default": {
|            "description": "successful operation"
|          }
|        }
|      }
|    }
|  }
|}""".stripMargin,
       true),
      ("""{
|  "swagger": "2.0",
| "info": {
|    "title": "Swagger Petstore",
|  },
|  "host": "petstore.swagger.io",
|  "basePath": "/v2",
|  "schemes": [
|    "https",
|    "http"
|  ],
|  "paths": {
|    "/path": {
|      "get": {
|        "responses": {
|          "default": {
|            "description": "successful operation"
|          }
|        }
|      }
|    }
|  }
|}""".stripMargin,
       false),
      ("""openapi: 3.0.1
|info:
|  title: Swagger Petstore
|  version: 1.0.5
|servers:
|- url: https://petstore.swagger.io/v2
|- url: http://petstore.swagger.io/v2
|tags:
|- name: pet
|  description: Everything about your Pets
|  externalDocs:
|    description: Find out more
|    url: http://swagger.io
|- name: store
|  description: Access to Petstore orders
|- name: user
|  description: Operations about user
|  externalDocs:
|    description: Find out more about our store
|    url: http://swagger.io
|paths:
|  /path:
|    get:
|      responses:
|        default:
|          description: successful operation
|          content: {}
|components: {}""".stripMargin,
       true),
      ("""openapi: 3.0.1
|info:
|  title: Swagger Petstore
|servers:
|- url: https://petstore.swagger.io/v2
|- url: http://petstore.swagger.io/v2
|tags:
|- name: pet
|  description: Everything about your Pets
|  externalDocs:
|    description: Find out more
|    url: http://swagger.io
|- name: store
|  description: Access to Petstore orders
|- name: user
|  description: Operations about user
|  externalDocs:
|    description: Find out more about our store
|    url: http://swagger.io
|paths:
|  /path:
|    get:
|      responses:
|        default:
|          description: successful operation
|          content: {}
|components: {}""".stripMargin,
       false)
    )
    forAll(properties) { (input, isValid) =>
      assertResult(isValid) {
        Validator.isValid(input)
      }
    }
  }

}
