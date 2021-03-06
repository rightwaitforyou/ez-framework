package com.ecfront.ez.framework.service.auth

import com.ecfront.common.Resp
import com.ecfront.ez.framework.core.EZServiceAdapter
import com.ecfront.ez.framework.core.interceptor.EZAsyncInterceptorProcessor
import com.ecfront.ez.framework.service.auth.model._
import com.ecfront.ez.framework.service.distributed.DMQService
import com.ecfront.ez.framework.service.rpc.foundation.AutoBuildingProcessor
import com.ecfront.ez.framework.service.rpc.http.{HTTP, HttpInterceptor}
import io.vertx.core.json.JsonObject

import scala.collection.JavaConversions._

object ServiceAdapter extends EZServiceAdapter[JsonObject] {

  private val EZ_EVENT_ORGANIZATION_INIT: String = "ez:event:organization_init"
  private val EZ_EVENT_LOGIN_SUCCESS: String = "ez:event:loginSuccess"
  private val EZ_EVENT_LOGOUT: String = "ez:event:logout"

  // 组织初始化事件
  var ezEvent_organizationInit: DMQService[String] = _
  // 登录成功事件
  var ezEvent_loginSuccess: DMQService[Token_Info_VO] = _
  // 注销事件
  var ezEvent_logout: DMQService[Token_Info_VO] = _

  var publicUriPrefix: String = _
  var allowRegister: Boolean = _
  var customLogin: Boolean = _
  var selfActive: Boolean = _
  var useRelTable: Boolean = _
  var defaultRoleFlag: String = _
  var defaultOrganizationCode: String = _
  var loginUrl: String = _
  var mongoStorage: Boolean = _
  var loginKeepSeconds: Long = _
  var activeKeepSeconds: Long = _
  var loginLimit_showCaptcha: Int = _
  var encrypt_algorithm: String = _
  var encrypt_salt: String = _

  override def init(parameter: JsonObject): Resp[String] = {
    publicUriPrefix = parameter.getString("publicUriPrefix", "/public/")
    allowRegister = parameter.getBoolean("allowRegister", false)
    customLogin = parameter.getBoolean("customLogin", false)
    val loginLimit = parameter.getJsonObject("loginLimit", new JsonObject())
    if (loginLimit.containsKey("showCaptcha")) {
      loginLimit_showCaptcha = loginLimit.getInteger("showCaptcha")
    } else {
      loginLimit_showCaptcha = Int.MaxValue
    }
    selfActive = parameter.getBoolean("selfActive", true)
    useRelTable = parameter.getBoolean("useRelTable", false)
    if (parameter.containsKey("customTables")) {
      parameter.getJsonObject("customTables").foreach {
        item =>
          item.getKey match {
            case "organization" => EZ_Organization.customTableName(item.getValue.asInstanceOf[String])
            case "account" => EZ_Account.customTableName(item.getValue.asInstanceOf[String])
            case "resource" => EZ_Resource.customTableName(item.getValue.asInstanceOf[String])
            case "role" => EZ_Role.customTableName(item.getValue.asInstanceOf[String])
            case "menu" => EZ_Menu.customTableName(item.getValue.asInstanceOf[String])
            case "rel_account_role" => EZ_Account.TABLE_REL_ACCOUNT_ROLE = item.getValue.asInstanceOf[String]
            case "rel_role_resource" => EZ_Role.TABLE_REL_ROLE_RESOURCE = item.getValue.asInstanceOf[String]
            case "rel_menu_role" => EZ_Menu.TABLE_REL_MENU_ROLE = item.getValue.asInstanceOf[String]
          }
      }
    }
    defaultOrganizationCode = parameter.getString("defaultOrganizationCode", EZ_Organization.DEFAULT_ORGANIZATION_CODE)
    defaultRoleFlag = parameter.getString("defaultRoleFlag", EZ_Role.USER_ROLE_FLAG)
    loginUrl = parameter.getString("loginUrl", "#/auth/login")
    loginKeepSeconds = parameter.getLong("loginKeepSeconds", 0L)
    activeKeepSeconds = parameter.getLong("activeKeepSeconds", 24L * 60 * 60)
    encrypt_algorithm =
      if (parameter.containsKey("encrypt") && parameter.getJsonObject("encrypt").containsKey("algorithm")) {
        parameter.getJsonObject("encrypt").getString("algorithm")
      } else {
        "SHA-256"
      }
    encrypt_salt =
      if (parameter.containsKey("encrypt") && parameter.getJsonObject("encrypt").containsKey("salt")) {
        parameter.getJsonObject("encrypt").getString("salt")
      } else {
        ""
      }
    EZ_Account.init(parameter.getString("extAccountStorage", null))
    if (!loginUrl.toLowerCase().startsWith("http")) {
      loginUrl = com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.webUrl + loginUrl
    }
    EZAsyncInterceptorProcessor.register(HttpInterceptor.category, AuthHttpInterceptor)
    AutoBuildingProcessor.autoBuilding[HTTP]("com.ecfront.ez.framework.service.auth", classOf[HTTP])

    ezEvent_organizationInit = DMQService[String](ServiceAdapter.EZ_EVENT_ORGANIZATION_INIT)
    ezEvent_loginSuccess = DMQService[Token_Info_VO](ServiceAdapter.EZ_EVENT_LOGIN_SUCCESS)
    ezEvent_logout = DMQService[Token_Info_VO](ServiceAdapter.EZ_EVENT_LOGOUT)

    Initiator.init()
    Resp.success("")
  }


  override def destroy(parameter: JsonObject): Resp[String] = {
    Resp.success("")
  }

  // 服务动态依赖处理方法，如果服务需要根据配置使用不同依赖请重写此方法
  override def getDynamicDependents(parameter: JsonObject): Set[String] = {
    mongoStorage = parameter.getString("storage", "mongo") == "mongo"
    val s = if (mongoStorage) {
      Set(com.ecfront.ez.framework.service.storage.mongo.ServiceAdapter.serviceName)
    } else {
      Set(com.ecfront.ez.framework.service.storage.jdbc.ServiceAdapter.serviceName)
    }
    if (parameter.containsKey("allowRegister") && parameter.getBoolean("allowRegister")) {
      Set(
        com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.serviceName,
        com.ecfront.ez.framework.service.email.ServiceAdapter.serviceName,
        com.ecfront.ez.framework.service.redis.ServiceAdapter.serviceName
      ) ++ s
    } else {
      Set(
        com.ecfront.ez.framework.service.rpc.http.ServiceAdapter.serviceName,
        com.ecfront.ez.framework.service.redis.ServiceAdapter.serviceName
      ) ++ s
    }
  }

  override var serviceName: String = "auth"

}


