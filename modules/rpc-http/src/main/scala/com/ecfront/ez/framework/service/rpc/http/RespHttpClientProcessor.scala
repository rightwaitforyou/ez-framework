package com.ecfront.ez.framework.service.rpc.http

import com.ecfront.common.{JsonHelper, Resp, StandardCode}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.vertx.core.http._
import io.vertx.core.json.{JsonArray, JsonObject}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * Resp返回值封装的HTTP 请求操作
  *
  * 包含了对HTTP GET POST PUT DELETE 四类常用操作
  *
  */
object RespHttpClientProcessor extends LazyLogging {

  /**
    * GET 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def get[E: Manifest](url: String, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    Await.result(Async.get[E](url, contentType), Duration.Inf)
  }

  /**
    * POST 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def post[E: Manifest](url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    Await.result(Async.post[E](url, body, contentType), Duration.Inf)
  }

  /**
    * PUT 请求
    *
    * @param url         请求URL
    * @param body        请求体
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def put[E: Manifest](url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    Await.result(Async.put[E](url, body, contentType), Duration.Inf)
  }

  /**
    * DELETE 请求
    *
    * @param url         请求URL
    * @param contentType 请求类型，默认为 application/json; charset=utf-8
    * @return 请求结果，string类型
    */
  def delete[E: Manifest](url: String, contentType: String = "application/json; charset=utf-8"): Resp[E] = {
    Await.result(Async.delete[E](url, contentType), Duration.Inf)
  }

  object Async {

    /**
      * GET 请求
      *
      * @param url         请求URL
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def get[E: Manifest](url: String, contentType: String = "application/json; charset=utf-8"): Future[Resp[E]] = {
      request[E](HttpMethod.GET, url, null, contentType)
    }

    /**
      * POST 请求
      *
      * @param url         请求URL
      * @param body        请求体
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def post[E: Manifest](url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Future[Resp[E]] = {
      request[E](HttpMethod.POST, url, body, contentType)
    }

    /**
      * PUT 请求
      *
      * @param url         请求URL
      * @param body        请求体
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def put[E: Manifest](url: String, body: Any, contentType: String = "application/json; charset=utf-8"): Future[Resp[E]] = {
      request[E](HttpMethod.PUT, url, body, contentType)
    }

    /**
      * DELETE 请求
      *
      * @param url         请求URL
      * @param contentType 请求类型，默认为 application/json; charset=utf-8
      * @return 请求结果，string类型
      */
    def delete[E: Manifest](url: String, contentType: String = "application/json; charset=utf-8"): Future[Resp[E]] = {
      request[E](HttpMethod.DELETE, url, null, contentType)
    }

    private def request[E](method: HttpMethod, url: String, body: Any, contentType: String)(implicit m: Manifest[E]): Future[Resp[E]] = {
      val p = Promise[Resp[E]]()
      HttpClientProcessor.Async.request(method, url, body, contentType).onSuccess {
        case resp =>
          val json = new JsonObject(resp)
          val code = json.getString("code")
          val result: Resp[E] =
            if (code == StandardCode.SUCCESS) {
              val jsonBody = json.getValue("body")
              if (jsonBody == null) {
                Resp[E](StandardCode.SUCCESS, "")
              } else {
                val body = jsonBody match {
                  case obj: JsonArray =>
                    m.runtimeClass match {
                      case mf if mf == classOf[JsonArray] =>
                        obj.asInstanceOf[E]
                      case _ =>
                        JsonHelper.toObject[E](obj.encode())
                    }
                  case obj: JsonObject =>
                    m.runtimeClass match {
                      case mf if mf == classOf[JsonObject] =>
                        obj.asInstanceOf[E]
                      case _ =>
                        JsonHelper.toObject[E](obj.encode())
                    }
                  case _ =>
                    jsonBody.asInstanceOf[E]
                }
                Resp.success[E](body)
              }
            } else {
              Resp[E](code, json.getString("message"))
            }
          p.success(result)
      }
      p.future
    }
  }

}

