package edu.umass.cs.automan.core.logging

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class CheckboxDistribMemoTest extends FlatSpec with Matchers {

  "A checkbox distribution program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(budget = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      log_verbosity = LogLevelDebug()
    )

    // test params
    val sample_size = 30
    val mock_answers = genAnswers(
      Array(Set('spongebob,'count), Set('spongebob), Set('count,'spongebob)),
      Array("0.25", "0.25", "0.50"),
      sample_size
    ).toList

    def which_one(text: String) = checkboxes (
      sample_size = sample_size,
      budget = 8.00,
      text = text,
      options = List(
        mt.Option('oscar, "Oscar the Grouch"),
        mt.Option('kermit, "Kermit the Frog"),
        mt.Option('spongebob, "Spongebob Squarepants"),
        mt.Option('cookie, "Cookie Monster"),
        mt.Option('count, "The Count")
      ),
      mock_answers = makeMocksAt(mock_answers.toList, 0)
    )

    def which_one2(text: String) = checkboxes (
      sample_size = sample_size,
      budget = 8.00,
      text = text,
      options = List(
        mt.Option('oscar, "Oscar the Grouch"),
        mt.Option('kermit, "Kermit the Frog"),
        mt.Option('spongebob, "Spongebob Squarepants"),
        mt.Option('cookie, "Cookie Monster"),
        mt.Option('count, "The Count")
      ),
      mock_answers = List()
    )

    automan(mt, test_mode = true, in_mem_db = true) {
      which_one("Which one of these does not belong?").answer match {
        case Answers(values, cost, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          compareDistributions(mock_answers, values) should be (true)
          cost should be (BigDecimal(0.06) * sample_size)
        case _ =>
          fail()
      }

      which_one2("Which one of these does not belong?").answer match {
        case Answers(values, cost, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          compareDistributions(mock_answers, values) should be (true)
          (cost == BigDecimal(0.00)) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
