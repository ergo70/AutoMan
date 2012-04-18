package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.{CheckboxAnswer, RadioButtonAnswer, Answer}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.question.{CheckboxQuestion, RadioButtonQuestion, Question}

abstract class ValidationStrategy {
  var _budget_committed: BigDecimal = 0.00
  var _confidence: Double = 0.95
  var _num_possibilities: BigInt = 2
  var _thunks = List[Thunk]()

  def confidence: Double = _confidence
  def confidence_=(c: Double) { _confidence = c }
  def current_confidence: Double
  def is_confident: Boolean
  def num_possibilities: BigInt = _num_possibilities
  def num_possibilities_=(n: BigInt) { _num_possibilities = n }
  def select_best(question: Question) : Answer = {
    // group by unique symbol specific to each answer type
    val valid_thunks = _thunks.filter{t =>
      t.state == SchedulerState.RETRIEVED ||
      t.state == SchedulerState.PROCESSED
    }

    if (valid_thunks.size == 0) {
      return question match {
        case rbq:RadioButtonQuestion[_] => new RadioButtonAnswer(None, "invalid", 'invalid)
        case cbq:CheckboxQuestion[_] => new CheckboxAnswer(None, "invalid", Set('invalid))
        case _ => throw new Exception("Question type not yet supported.")
      }
    }

    val groups = valid_thunks.groupBy { t => t.answer.comparator }
    
    println("DEBUG: STRATEGY: Groups = " + groups)

    // find the grouping symbol of the largest group
    val gsymb = groups.maxBy { case(opt, as) => as.size }._1

    println("DEBUG: STRATEGY: Symbol of largest group is " + gsymb)
    println("DEBUG: STRATEGY: classOf Thunk.answer is " + groups(gsymb).head.answer.getClass)

    // return an Answer
    groups(gsymb).head.answer match {
      case rba: RadioButtonAnswer => new RadioButtonAnswer(Some(current_confidence), "aggregated", rba.value)
      case cba: CheckboxAnswer => new CheckboxAnswer(Some(current_confidence), "aggregated", cba.values)
      case _ => throw new Exception("Question type not yet supported.")
    }
  }
  def spawn(question: Question, suffered_timeout: Boolean): List[Thunk]
}