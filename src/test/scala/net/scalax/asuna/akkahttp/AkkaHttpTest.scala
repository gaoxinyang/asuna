package net.scalax.test01

import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.testkit.ScalatestRouteTest
import net.scalax.asuna.akkahttp.AkkaHttpParameterHelper
import org.scalatest._

import scala.concurrent.{ Await, Future, duration }

case class Model(id: Int, name: String, age: Int, nick: String, field1: String, field2: Long, field3: String, extCookieField: Map[String, String])

class AkkaHttpTest
  extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with AkkaHttpParameterHelper {

  class ParameterTable(cookieKeys: List[String]) {
    self =>

    import akka.http.scaladsl.server._
    import Directives._

    //常量字段
    def id = helper.LiteralColumn(-1)
    //String 类型字段
    def name = helper.cookie
    //其他需要 Unmarshaller 的字段
    def age = helper.formFieldAs[Int]
    //String 类型字段，Url 参数
    def nick = helper.parameter

    //field1 field2 field3 是表单中的字段，因为属性 key 一样可以用默认的 implicit 自动生成

    //Map[String, String] 类型的动态字段，可以与其他逻辑匹配生成
    def extCookieField = akkahttp.shaped(cookieKeys.map(key => formField(key).map(value => Tuple1((key, value))))).dmap(_.toMap)

    //根据 asnua 生成的自定义 Directive1
    def cusDirective: Directive1[Model] = akkahttp.effect(akkahttp.caseOnly[ParameterTable, Model](self)).toDirective

  }

  def await[A](f: Future[A]) = Await.result(f, duration.Duration.Inf)

  import akka.http.scaladsl.server._
  import Directives._

  val route =
    path("formtest") {
      post {
        new ParameterTable(List("cKey1", "cKey2")).cusDirective { model =>
          complete(model.toString)
        }
      }
    } ~ path("index") {
      get {
        complete("hello")
      }
    }

  "akkahttp's shape should auto shape values" in {
    import akka.http.scaladsl.model._
    Post(
      "/formtest?nick=nick_value",
      FormData(
        ("age", "3456"),
        ("field1", "field1Value"),
        ("field2", "52345234"),
        ("field3", "field3Value"),
        ("cKey1", "cKey1Value"),
        ("cKey2", "cKey2Value"),
        ("cKey3", "NotUsed"))) ~>
      Cookie("name", "name_cookie_value") ~>
      route ~>
      check {
        responseAs[String] shouldEqual
          Model(
            id = -1,
            name = "name_cookie_value",
            age = 3456, nick = "nick_value",
            field1 = "field1Value",
            field2 = 52345234L,
            field3 = "field3Value",
            extCookieField = Map(
              ("cKey1", "cKey1Value"),
              ("cKey2", "cKey2Value"))).toString
      }
  }

}