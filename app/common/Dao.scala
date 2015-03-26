package common

import com.avaje.ebean.{Query, Ebean}

/**
 * Created by hkatz on 3/20/15.
 */
abstract class Dao[T](cls:Class[T]) {

  /**
   * Find by Id
   */
  def find(id:Any):Option[T] = {
    return Option(Ebean.find(cls, id))
  }

  /**
   * find with expressions and joins etc
   */
  def find(): Query[T] = {
    return Ebean.find(cls)
  }

  /**
   * return a reference
   */
  def ref(id:Any):T = {
    return Ebean.getReference(cls, id)
  }

  /**
   * Save (insert)
   */
  def save(o:Any):Unit = {
    Ebean.save(o)
  }

  /**
   * Save (update)
   */
  def update(o:Any):Unit = {
    Ebean.update(o)
  }

  /**
   * Delete
   */
  def delete(o:Any):Unit = {
    Ebean.delete(o)
  }
}
