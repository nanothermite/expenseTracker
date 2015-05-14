package common

import scala.reflect._
import scala.reflect.runtime.universe._

/**
 * Created by hkatz on 5/13/15.
 */
trait myTypes {
  def getType[T: TypeTag](obj: T) = typeOf[T]
  def getTypeTag[T: TypeTag](obj: T) = typeTag[T]
  def getClassTag[T: ClassTag](obj: T) = classTag[T]
}
