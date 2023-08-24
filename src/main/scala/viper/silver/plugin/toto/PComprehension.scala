package viper.silver.plugin.toto

import viper.silver.ast.{Exp, Position}
import viper.silver.parser._
import viper.silver.plugin.toto.PComprehension.getNewTypeVariable

case class PComprehension(opUnit: PCall, mappingFieldReceiver: PMappingFieldReceiver, filter: PExp)(val pos: (Position, Position)) extends PExtender with PExp {
  override val getSubnodes: Seq[PNode] = Seq(opUnit, mappingFieldReceiver, filter)

  override def typecheck(t: TypeChecker, n: NameAnalyser): Option[Seq[String]] = {
    var messagesOut : Seq[String] = Seq()

    // Check type of filter, must be a Set
    t.checkTopTyped(filter, Some(PSetType(getNewTypeVariable("CompSet"))()))
    val setType: PType = filter.typ match {
      case PSetType(e) => e
      case _ =>
        messagesOut = messagesOut :+ "Filter should of Set[...] type."
        return Some(messagesOut)
    }

    // Check type of unit
//    t.checkTopTyped(unit, None)

    // Check type of op, must be an Operator with unit as argument
//    val correctOpType = ComprehensionPlugin.makeDomainType("Operator", Seq(unit.typ))
    t.checkTopTyped(opUnit, Some(ComprehensionPlugin.
      makeDomainType("Operator", Seq(getNewTypeVariable("CompOp")))))

    opUnit.typ match {
      case pd: PDomainType if pd.domain.name == DomainsGenerator.opDKey =>
        typ = pd.typeArguments.head
      case _ =>
        messagesOut = messagesOut :+ "Operator should of Operator[_] type."
        return Some(messagesOut)
    }

    // Check type of mappingFieldReceiver
    mappingFieldReceiver.typecheckComp(t, n, typ, setType)

    // Set type of this node
    Some(messagesOut)
  }

  override def typecheck(t: TypeChecker, n: NameAnalyser, expected: PType): Option[Seq[String]] = {
    // this calls t.checkTopTyped, which will call checkInternal, which calls the above typecheck
    t.checkTopTyped(this, Some(expected))
    None
  }

  val _typeSubstitutions: Seq[PTypeSubstitution] = Seq(PTypeSubstitution.id)

  override def typeSubstitutions: collection.Seq[PTypeSubstitution] = Seq()

  override def forceSubstitution(ts: PTypeSubstitution): Unit = ()

  // Translate the parser node into an AST node
  override def translateExp(t: Translator): Exp = {
    val opTranslated = t.exp(opUnit)
//    val unitTranslated = t.exp(unit)
    val (mappingOpt, fieldString, receiverTranslated) = mappingFieldReceiver.translateTo(t)
    val filterTranslated = t.exp(filter)
//    val mappingTranslated = mappingOpt.getOrElse(throw new Exception("Mapping should be defined."))

    val tuple = AComprehension4Tuple(receiverTranslated, mappingOpt, opTranslated)(pos._1)
    val snap = ASnapshotApp(tuple, filterTranslated, fieldString)(pos._1)
//    val f = snap.filter
    AEvalComp(tuple, snap)(pos._1)
  }

}

object PComprehension {
  private var counter = 0
  private def increment(): Int = {
    counter += 1
    counter
  }

  private def getNewTypeVariable(name: String): PDomainType = {
    PTypeVar(s"$name#" + increment())
  }

}
