package com.asto.ez.framework.auth

import com.asto.ez.framework.MockStartupSpec
import com.asto.ez.framework.rpc.Method
import com.asto.ez.framework.rpc.http.HttpClientProcessor
import com.ecfront.common.{AsyncResp, Resp, StandardCode}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

class OrganizationSpec extends MockStartupSpec {

  test("Organization test") {

    EZ_Organization.deleteByCond(s"""{"code":"org1"}""")
    EZ_Resource.deleteByCond(s"""{"code":"GET@/org/1/foo/"}""")
    EZ_Role.deleteByCond(s"""{"code":"org1@user1"}""")
    EZ_Account.deleteByCond(s"""{"login_id":"u1"}""")

    val org = EZ_Organization("org1","组织1")
    Await.result(EZ_Organization.save(org), Duration.Inf)

    val res = EZ_Resource(Method.GET,"/org/1/foo/",s"Fetch org1 Info")
    Await.result(EZ_Resource.save(res), Duration.Inf)

    val role = EZ_Role()
    role.flag = "user1"
    role.name = "User 1"
    role.organization_code = "org1"
    role.enable = true
    role.resource_codes = List(
      s"GET@/org/1/foo/",
      s"GET@/auth/manage/account/"
    )
    Await.result(EZ_Role.save(role), Duration.Inf)

    var account = EZ_Account()
    account.login_id = "u1"
    account.name = "u1"
    account.email = "net@sunisle.org"
    account.password = "123"
    account.organization_code = "org1"
    account.enable = true
    account.role_codes = List(
      "org1@user1"
    )
    Await.result(EZ_Account.save(account), Duration.Inf)

    //login
    val loginP = Promise[Resp[Token_Info_VO]]()
    AuthService.doLogin("u1", "123", AsyncResp(loginP))
    val loginResp = Await.result(loginP.future, Duration.Inf)
    assert(loginResp
      && loginResp.body.token != ""
      && loginResp.body.login_id == "u1"
      && loginResp.body.organization_code == "org1"
    )
    val token = loginResp.body.token
    val loginInfoP = Promise[Resp[Token_Info_VO]]()
    AuthService.doGetLoginInfo(token, AsyncResp(loginInfoP))
    val loginInfoResp = Await.result(loginInfoP.future, Duration.Inf)
    assert(loginInfoResp
      && loginInfoResp.body.token != ""
      && loginInfoResp.body.login_id == "u1"
      && loginInfoResp.body.organization_code == "org1"
    )
    account = Await.result(EZ_Account.getByLoginId(loginInfoResp.body.login_id), Duration.Inf).body
    assert(Await.result(HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/${account.id}/?__ez_token__=$token", classOf[EZ_Account]), Duration.Inf).code == StandardCode.UNAUTHORIZED)
    assert(Await.result(HttpClientProcessor.get(s"http://127.0.0.1:8080/auth/manage/account/?__ez_token__=$token", classOf[List[EZ_Account]]), Duration.Inf))
    assert(Await.result(HttpClientProcessor.get(s"http://127.0.0.1:8080/org/1/foo/?__ez_token__=$token", classOf[List[Void]]), Duration.Inf).code==StandardCode.NOT_IMPLEMENTED)

  }

}


