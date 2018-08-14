package net.scalax.asuna.helper.encoder.macroImpl

import java.util.UUID

import net.scalax.asuna.core.encoder.{ EncoderShape, EncoderShapeValue }
import net.scalax.asuna.helper.{ MacroColumnInfo, MacroColumnInfoImpl }
import net.scalax.asuna.helper.decoder.macroImpl.{ ModelGen, PropertyType }
import net.scalax.asuna.helper.encoder.{ EncoderHelper, ForTableInput }
import shapeless.Lazy

import scala.reflect.macros.blackbox.Context

object EncoderMapper {

  class ProEncoderWrap[RepCol, DataCol]() {

    def propertySv[A, B, C](rep: A, propertyType: PropertyType[B])(implicit shape: Lazy[EncoderShape[A, B, C, RepCol, DataCol]]): EncoderShapeValue[Any, RepCol, DataCol] = {
      val rep1 = rep
      val shape1 = shape.value
      new EncoderShapeValue[B, RepCol, DataCol] {
        override type RepType = C
        override val rep = shape1.wrapRep(rep1)
        override val shape = shape1.packed
      }.asInstanceOf[EncoderShapeValue[Any, RepCol, DataCol]]
    }

  }

  object ProEncoderWrap {
    def value[RepCol, DataCol]: ProEncoderWrap[RepCol, DataCol] = new ProEncoderWrap[RepCol, DataCol]()
  }

  class EncoderMapperImpl(val c: Context) {
    self =>

    import c.universe._

    case class FieldNames(law: String, shapeValueName: String)

    def commonProUseInShape[RepCol: c.WeakTypeTag, DataCol: c.WeakTypeTag, Table: c.WeakTypeTag, Model: c.WeakTypeTag](mgVar: String, proEncoderVar: String, fieldName: FieldNames, modelName: TermName, isMissingField: Boolean) = {
      val traitName = c.freshName(fieldName.law)
      val defName = "defName" + UUID.randomUUID.toString.replaceAllLiterally("-", "a")
      //val encoderShape = weakTypeOf[EncoderShape[_, _, _, _, _]]
      val repCol = weakTypeOf[RepCol]
      val dataCol = weakTypeOf[DataCol]
      val edsv = weakTypeOf[EncoderShapeValue[Any, RepCol, DataCol]]

      //val columnInfo = weakTypeOf[MacroColumnInfo]
      //val columnInfoImpl = weakTypeOf[MacroColumnInfoImpl]
      //val propertyType = weakTypeOf[PropertyType[_]]

      /*
      def ${TermName(defName)}[A, B, C](rep: A, propertyType: _root_.net.scalax.asuna.helper.decoder.macroImpl.PropertyType[B])(implicit shape: _root_.net.scalax.asuna.core.encoder.EncoderShape[A, B, C, $repCol, $dataCol]) = {
            val rep1 = rep
            val shape1 = shape
            new _root_.net.scalax.asuna.core.encoder.EncoderShapeValue[B, $repCol, $dataCol] {
              override type RepType = C
              override val rep = shape1.wrapRep(rep1)
              override val shape = shape1.packed
            }
          }
       */

      //内置 implicit 查找方法
      val q = q"""
        val ${TermName(fieldName.shapeValueName)} = {
            def ${TermName(defName)}[A, B, C](rep: A, propertyType: _root_.net.scalax.asuna.helper.decoder.macroImpl.PropertyType[B])(implicit shape: _root_.net.scalax.asuna.core.encoder.EncoderShape[A, B, C, $repCol, $dataCol]) = {
            ${TermName(proEncoderVar)}.propertySv(rep, propertyType)(shape)
        }

          def ${TermName(traitName)}(implicit columnInfo: _root_.net.scalax.asuna.helper.MacroColumnInfo): $edsv = {
            ${
        if (isMissingField) {
          //q"""${TermName(defName)}(${TermName(mgVar)}(_.${TermName(fieldName)}).toPlaceholder, ${TermName(mgVar)}(_.${TermName(fieldName)})).emap((s: Any) => ${TermName(mgVar)}(_.${TermName(fieldName)}).convertData(s))"""
          //q"""${TermName(defName)}(${TermName(mgVar)}(_.${TermName(fieldName.law)}).toPlaceholder, ${TermName(mgVar)}(_.${TermName(fieldName.law)}))"""
          q"""${TermName(proEncoderVar)}.propertySv(${TermName(mgVar)}(_.${TermName(fieldName.law)}).toPlaceholder, ${TermName(mgVar)}(_.${TermName(fieldName.law)}))"""
        } else {
          //q"""${TermName(defName)}(${modelName}.${TermName(fieldName)}, ${TermName(mgVar)}(_.${TermName(fieldName)})).emap((s: Any) => ${TermName(mgVar)}(_.${TermName(fieldName)}).convertData(s))"""
          //q"""${TermName(defName)}(${modelName}.${TermName(fieldName.law)}, ${TermName(mgVar)}(_.${TermName(fieldName.law)}))"""
          q"""${TermName(proEncoderVar)}.propertySv(${modelName}.${TermName(fieldName.law)}, ${TermName(mgVar)}(_.${TermName(fieldName.law)}))"""
        }
      }
          }

          ${TermName(traitName)}(_root_.net.scalax.asuna.helper.MacroColumnInfoImpl(
            tableColumnName = ${Literal(Constant(fieldName.law))},
            modelColumnName = ${Literal(Constant(fieldName.law))}
          ))
        }
    """
      q
    }

    def impl[Table: c.WeakTypeTag, Case: c.WeakTypeTag, RepCol: c.WeakTypeTag, DataCol: c.WeakTypeTag]: c.Expr[ForTableInput[Table, Case, RepCol, DataCol]] = {
      val caseClass = weakTypeOf[Case]
      val table = weakTypeOf[Table]
      val repCol = weakTypeOf[RepCol]
      val dataCol = weakTypeOf[DataCol]
      //val modelGen = weakTypeOf[ModelGen[Case]]
      val allHelper = weakTypeOf[EncoderHelper[RepCol, DataCol]]
      val forTableInput = weakTypeOf[ForTableInput[Table, Case, RepCol, DataCol]]
      println("11" * 100)
      println(repCol)
      println(dataCol)

      //val forTableInputImpl = weakTypeOf[ForTableInputImpl[Table, Case, RepCol, DataCol]]
      val shapeValue = weakTypeOf[EncoderShapeValue[Case, RepCol, DataCol]]
      //val proType = weakTypeOf[ProEncoderWrap[RepCol, DataCol]]

      val mgVar = "mg" + UUID.randomUUID.toString.replaceAllLiterally("-", "a")
      val proEncoderWrap = "proEncoderWrap" + UUID.randomUUID.toString.replaceAllLiterally("-", "a")
      val encoderHelper = "encoderHelper" + UUID.randomUUID.toString.replaceAllLiterally("-", "a")

      val modelFieldNames = caseClass.members.filter { s => s.isTerm && s.asTerm.isCaseAccessor && s.asTerm.isVal }.map(_.name).collect { case TermName(n) => n.trim }.toList.map(s => FieldNames(law = s, shapeValueName = s + UUID.randomUUID.toString.replaceAllLiterally("-", "a")))
      val fieldNamesInTable = table.members.filter { s => s.isTerm && (s.asTerm.isVal || s.asTerm.isVar || s.asTerm.isMethod) }.map(_.name).collect { case TermName(n) => n.trim }.toList
      val misFieldsInTable = modelFieldNames.filter(n => !fieldNamesInTable.contains(n.law))

      def mgDef = List(
        q"""
        val ${TermName(mgVar)}: _root_.net.scalax.asuna.helper.decoder.macroImpl.ModelGen[$caseClass] = _root_.net.scalax.asuna.helper.decoder.macroImpl.ModelGen.value[$caseClass]
        """,
        q"""
        val ${TermName(proEncoderWrap)}: _root_.net.scalax.asuna.helper.encoder.macroImpl.EncoderMapper.ProEncoderWrap[$repCol, $dataCol] = _root_.net.scalax.asuna.helper.encoder.macroImpl.EncoderMapper.ProEncoderWrap.value[$repCol, $dataCol]
        """,
        q"""
        val ${TermName(encoderHelper)}: _root_.net.scalax.asuna.helper.encoder.EncoderHelper[$repCol, $dataCol] = _root_.net.scalax.asuna.helper.encoder.EncoderHelper.value[$repCol, $dataCol]
        """)

      def toShape1111(namePare: List[FieldNames]) = {
        val proNames = namePare
        val termVar1 = c.freshName("termVar1")
        val listSymbol = weakTypeOf[List[_]].typeSymbol.companion

        val toListTree = proNames.foldRight(q"""Nil""": Tree) { (name, tree) =>
          q"""${TermName(termVar1)}.${TermName(name.law)} :: $tree"""
        }

        val func = q"""
        { (${TermName(termVar1)}: $caseClass) =>
          $toListTree
        }
        """

        val q = q"""
        ${TermName(encoderHelper)}.shaped(
          _root_.scala.List.apply(..${proNames.map(eachName => q"""${TermName(eachName.shapeValueName)}""")})
        ).emap($func)
        """
        q
      }

      val q = c.Expr[ForTableInput[Table, Case, RepCol, DataCol]] {
        val repModelTermName = c.freshName
        val aa = weakTypeOf[ForTableInput[Table, Case, RepCol, DataCol]]
        q"""
          new $aa {
           override def input(${TermName(repModelTermName)}: $table):$shapeValue = {
           ..$mgDef
          ..${modelFieldNames.map { proName => commonProUseInShape[RepCol, DataCol, Table, Case](mgVar = mgVar, proEncoderVar = proEncoderWrap, fieldName = proName, modelName = TermName(repModelTermName), isMissingField = misFieldsInTable.contains(proName)) }}
           ${toShape1111(modelFieldNames)}
            }
          }: ${weakTypeOf[ForTableInput[Table, Case, RepCol, DataCol]]}
        """
      }
      println(q + "\n" + "22" * 100)
      q
    }

  }

}