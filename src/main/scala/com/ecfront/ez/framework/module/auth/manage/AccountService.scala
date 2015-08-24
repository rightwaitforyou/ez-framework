package com.ecfront.ez.framework.module.auth.manage

import com.ecfront.common.{EncryptHelper, Resp}
import com.ecfront.ez.framework.module.auth.Account
import com.ecfront.ez.framework.module.core.{CommonUtils, EZReq}
import com.ecfront.ez.framework.rpc._
import com.ecfront.ez.framework.service.protocols.JDBCService
import com.ecfront.ez.framework.service.{BasicService, SyncService}
import com.ecfront.storage.PageModel

@RPC("/auth/manage/account/")
@HTTP
object AccountService extends JDBCService[Account, EZReq] with SyncService[Account, EZReq] with BasicService {

  @POST("")
  def save(parameter: Map[String, String], body: Account, req: Option[EZReq]): Resp[String] = {
    _save(body, req)
  }

  @PUT(":id/")
  def update(parameter: Map[String, String], body: Account, req: Option[EZReq]): Resp[String] = {
    _update(parameter("id"), body, req)
  }

  @DELETE(":id/")
  def delete(parameter: Map[String, String], req: Option[EZReq]): Resp[String] = {
    _deleteById(parameter("id"), req)
  }

  @GET(":id/")
  def get(parameter: Map[String, String], req: Option[EZReq]): Resp[Account] = {
    _getById(parameter("id"), req)
  }

  @GET("page/:number/:size/")
  def page(parameter: Map[String, String], req: Option[EZReq]): Resp[PageModel[Account]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _pageByCondition(orderSql, Some(orderParams), parameter("number").toInt, parameter("size").toInt, req)
    } else {
      _pageAll(parameter("number").toInt, parameter("size").toInt, req)
    }
  }

  @GET("")
  def find(parameter: Map[String, String], req: Option[EZReq]): Resp[List[Account]] = {
    val (orderSql, orderParams) = CommonUtils.packageOrder(parameter)
    if (orderSql.nonEmpty) {
      _findByCondition(orderSql, Some(orderParams), req)
    } else {
      _findAll(req)
    }
  }

  /**
   * ID检查，是否非法
   * 设置主键、密码
   */
  override protected def _preSave(model: Account, request: Option[EZReq]): Resp[Any] = {
    if (model.id == null || model.id.trim.isEmpty) {
      Resp.badRequest("Require LoginId.")
    } else {
      model.password = packageEncryptPwd(model.id, model.password)
      Resp.success(model)
    }
  }

  override protected def _preUpdate(id: String, model: Account, request: Option[EZReq]): Resp[Any] = {
    model.password = packageEncryptPwd(model.id, model.password)
    Resp.success(model)
  }

  def packageEncryptPwd(loginId: String, password: String): String = {
    EncryptHelper.encrypt(loginId + password)
  }

}