package org.scalax.asuna.mapper.append

import scala.annotation.tailrec
import scala.collection.immutable.Queue

import scala.language.experimental.macros

class ApplyProperty[Model] {
  def p[P](u: Model => P): ItemTag[P] = new ItemTag[P]
}

object ApplyProperty {
  def apply[Model]: ApplyProperty[Model] = new ApplyProperty[Model]
}

trait TreeActorMessage {
  def actorIndex: Int
}

trait TreeContent {

  val c: scala.reflect.macros.blackbox.Context

  import c.universe._

  @tailrec
  final def applyValue(append: Vector[Tree], t: List[Tree]): Vector[Tree] = {
    t match {
      case i1 :: i2 :: i3 :: i4 :: tail =>
        applyValue(append.+:(q"""org.scalax.asuna.mapper.append.Item.apply4(${i1}, ${i2}, ${i3}, ${i4})"""), tail)
      case i1 :: i2 :: i3 :: Nil =>
        append.+:(q"""org.scalax.asuna.mapper.append.Item.apply3(${i1}, ${i2}, ${i3})""")
      case i1 :: i2 :: Nil =>
        append.+:(q"""org.scalax.asuna.mapper.append.Item.apply2(${i1}, ${i2})""")
      case i1 :: Nil =>
        append.+:(q"""org.scalax.asuna.mapper.append.Item.apply1(${i1})""")
      case Nil => append
    }
  }

  def applyValue1(t: List[Tree]): Vector[Tree] = {
    applyValue(Vector.empty, t)
  }

  @tailrec
  final def applyValue2(t: List[Tree]): Tree = {
    t match {
      case Nil =>
        q"""org.scalax.asuna.mapper.append.Item.apply0"""
      case head :: Nil =>
        head
      case l =>
        applyValue2(applyValue1(l).toList)
    }
  }

  @tailrec
  final def applyTag(append: Vector[Tree], t: List[Tree]): Vector[Tree] = {
    t match {
      case i1 :: i2 :: i3 :: i4 :: tail =>
        applyTag(append.+:(q"""org.scalax.asuna.mapper.append.Item.applyTag4(${i1}, ${i2}, ${i3}, ${i4})"""), tail)
      case i1 :: i2 :: i3 :: Nil =>
        append.+:(q"""org.scalax.asuna.mapper.append.Item.applyTag3(${i1}, ${i2}, ${i3})""")
      case i1 :: i2 :: Nil =>
        append.+:(q"""org.scalax.asuna.mapper.append.Item.applyTag2(${i1}, ${i2})""")
      case i1 :: Nil =>
        append.+:(q"""org.scalax.asuna.mapper.append.Item.applyTag1(${i1})""")
      case Nil => append
    }
  }

  def applyTag1(t: List[Tree]): Vector[Tree] = {
    applyTag(Vector.empty, t)
  }

  @tailrec
  final def applyTag2(t: List[Tree]): Tree = {
    t match {
      case Nil =>
        q"""org.scalax.asuna.mapper.append.Item.applyTag0"""
      case head :: Nil =>
        head
      case l =>
        applyTag2(applyTag1(l).toList)
    }
  }

  trait ContextTreeActor {
    subSelf =>

    def selfIndex: Int
    def init: Option[TreeActorMessage]
    def isSelfMessage(message: TreeActorMessage): Boolean = subSelf.selfIndex == message.actorIndex
    def tree1: Tree
    def tree2: Tree
    def typer: Option[List[Tree]]
    def receive: PartialFunction[TreeActorMessage, (Option[TreeActorMessage], ContextTreeActor)]

  }

  final def zipTrees(tree: List[List[Tree]]): List[List[Tree]] = {
    val (line, tailtail) = tree.map { t =>
      if (t.isEmpty) {
        (Option.empty, Nil)
      } else {
        (Option(t.head), t.tail)
      }
    }.unzip

    val ll = if (line.forall(_.isDefined)) {
      line.map(_.get) :: zipTrees(tailtail)
    } else {
      List.empty[List[Tree]]
    }
    ll
  }

  def zipTrees1(tree: List[ContextTreeActor]): Tree = {
    q"""org.scalax.asuna.mapper.append.Item.虚得一逼(${applyTag2(zipTrees(tree.flatMap(_.typer)).map(s => q"""(..${s})"""))})"""
  }

  @tailrec
  final def sendActor(l: List[ContextTreeActor], messages: Queue[TreeActorMessage]): List[ContextTreeActor] = {
    messages.dequeueOption match {
      case Some((head, q)) =>
        val (messageList, newActors) = l.map { item =>
          val (messageOpt, newItem) = if (item.receive.isDefinedAt(head)) {
            item.receive.apply(head)
          } else {
            (Option.empty, item)
          }
          (messageOpt, newItem)
        }.unzip
        sendActor(newActors, q.enqueue(messageList.collect { case Some(s) => s }))
      case _ =>
        l
    }

  }

  def sendActor1(l: List[ContextTreeActor]): List[ContextTreeActor] = sendActor(l, Queue.empty)

}

trait ModelApply[H] {
  type M
  type P
  type Str
  def p: (H => M, Str, ItemTag[P])
}

object ModelApply {

  type Aux[H, MM, PP, SS] = ModelApply[H] { type M = MM; type P = PP; type Str = SS }

  class ModelApplyApply[H] {
    def model[MM, PP, SS](pp: (H => MM, SS, ItemTag[PP])): ModelApply.Aux[H, MM, PP, SS] = new ModelApply[H] {
      override type M   = MM
      override type P   = PP
      override type Str = SS
      override def p = pp
    }
  }
  def instance[M]: ModelApplyApply[M] = new ModelApplyApply[M]

  implicit def appendMacroImpl[H, M, P, S]: ModelApply.Aux[H, M, P, S] = macro AppendMacro.AppendMacroImpl2.generic[H, M, P, S]

}

object AppendMacro {

  class AppendMacroImpl2(override val c: scala.reflect.macros.whitebox.Context) extends AppendMacroImpl1

  trait AppendMacroImpl1 extends TreeContent {

    override val c: scala.reflect.macros.blackbox.Context

    import c.universe._

    trait ShapelessGenericGetter extends ContextTreeActor {
      subSelf =>

      def weakTypeTag: Type
      def valName: String
      override def init: Option[TreeActorMessage] = Option.empty

      def names: List[String] =
        weakTypeTag.members.toList
          .filter { s =>
            s.isTerm && s.asTerm.isVal && s.asTerm.isCaseAccessor
          }
          .map(s => (s.name, s))
          .collect {
            case (TermName(n), s) =>
              val proName = n.trim
              proName
          }

      override def tree1: Tree = {
        q"""{ ${TermName(valName)}: ${weakTypeTag} =>
          ${applyValue2(names.map(name => q"""${TermName(valName)}.${TermName(name)}"""))}
        }"""
      }

      override def tree2: Tree = {
        q"""${applyValue2(names.map(name => q"""${Literal(Constant(name))}"""))}"""
      }

      override def typer: Option[List[Tree]] =
        Option(names.map(name => q"""org.scalax.asuna.mapper.append.ApplyProperty[${weakTypeTag}].p(_.${TermName(name)})"""))
      override def receive: PartialFunction[TreeActorMessage, (Option[TreeActorMessage], ContextTreeActor)] = {
        case _ => (Option.empty, subSelf)
      }

    }

    class ShapelessGenericGetterImpl(override val valName: String, override val selfIndex: Int, override implicit val weakTypeTag: Type)
        extends ShapelessGenericGetter

    def generic[H: c.WeakTypeTag, P: c.WeakTypeTag, M: c.WeakTypeTag, Str: c.WeakTypeTag]: c.Expr[ModelApply.Aux[H, M, P, Str]] = {
      val h = c.weakTypeOf[H]

      val shapelessGetter = new ShapelessGenericGetterImpl("ll", 1, h)
      val result          = sendActor1(List(shapelessGetter))
      val List(item)      = result

      c.Expr[ModelApply.Aux[H, M, P, Str]] {
        q"""org.scalax.asuna.mapper.append.ModelApply.instance[${h}].model((${item.tree1}, ${item.tree2}, ${zipTrees1(result)}))"""
      }
    }

  }

}
