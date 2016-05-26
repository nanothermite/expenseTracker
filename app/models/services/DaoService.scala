package models.services

import common.myTypes
import com.avaje.ebean._
import utils.Sha256

import scala.collection.{immutable => im, mutable => mu}
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

trait DaoService {
  def methodByReflectionO[T : TypeTag](name: String, m: ru.Mirror, tru: Type): MethodMirror

  def genSql(query: String, colMap: im.Map[String, String]): RawSql

  def getList[T : TypeTag](getter: String, query: String, colMap: im.Map[String, String],
                           pList: im.Map[String, AnyRef], t: T): List[T]
}

class DaoServiceImpl extends DaoService with myTypes with Sha256 {

  val m = ru.runtimeMirror(getClass.getClassLoader)

  /**
    * reflection on Object
    *
    * @param name method
    * @param m    runtime
    * @param tru  reflection
    * @tparam T   internals
    * @return
    */
  def methodByReflectionO[T : TypeTag](name: String, m: ru.Mirror, tru: Type): MethodMirror = {
    val modX = tru.termSymbol.asModule
    val methodX = tru.decl(ru.TermName(name)).asMethod
    val mm = m.reflectModule(modX)
    val im = m.reflect(mm.instance)
    im.reflectMethod(methodX)
  }

  def genSql(query: String, colMap: im.Map[String, String]): RawSql = {
    val rawSqlBld = RawSqlBuilder.parse(query)
    for ((k, v) <- colMap) {
      rawSqlBld.columnMapping(k, v)
    }
    rawSqlBld.create()
  }

  /**
    * generate collection of T objects using getter
    *
    * @param getter method to invoke
    * @param query  actual
    * @param colMap for ebean
    * @param pList  parameters to query
    * @param t      for reflection
    * @tparam T     reflection on return type
    * @return
    */
  def getList[T : TypeTag](getter: String, query: String, colMap: im.Map[String, String],
                           pList: im.Map[String, AnyRef], t: T): List[T] = {
    val rawSql = genSql(query, colMap)
    val myType = getType(t)
    val obj = methodByReflectionO[T](getter, m, myType)
    obj(rawSql, Some(pList)).asInstanceOf[List[T]]
  }
}
