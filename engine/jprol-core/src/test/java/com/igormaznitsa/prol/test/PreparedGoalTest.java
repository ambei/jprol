package com.igormaznitsa.prol.test;

import com.igormaznitsa.prol.data.Term;
import com.igormaznitsa.prol.data.TermInteger;
import com.igormaznitsa.prol.io.DefaultProlStreamManagerImpl;
import com.igormaznitsa.prol.logic.ChoicePoint;
import com.igormaznitsa.prol.logic.DeferredGoal;
import com.igormaznitsa.prol.logic.ProlContext;
import com.igormaznitsa.prol.parser.ProlConsult;
import org.junit.Test;

import static org.junit.Assert.*;

public class PreparedGoalTest extends AbstractProlTest {

  @Test
  public void testPreparedGoal() throws Exception {
    final ProlContext context = new ProlContext("PreparedGoal test", DefaultProlStreamManagerImpl.getInstance());
    final ProlConsult consult = new ProlConsult("test(A,B,C,D,E):-E is A+B/C*D. randomval(A):-rnd([1,2,3,4,5],A).", context);
    consult.consult();

    final DeferredGoal goal = new DeferredGoal("test({?},{?},{?},{?},Result).", context);

    for (int a = 1; a < 100; a += 4) {
      for (int b = 1; b < 60; b += 3) {
        for (int c = 2; c < 8; c++) {
          for (int d = 1; d < 5; d++) {
            final int precalculatedResult = a + b / c * d;

            final ChoicePoint workGoal = goal.forIntegerParameters(a, b, c, d);
            final Term resultTerm = workGoal.next();

            assertEquals(workGoal.getVarAsNumber("Result").intValue(), precalculatedResult);
          }
        }
      }
    }

    final DeferredGoal oncegoal = new DeferredGoal("randomval(Result).", context);

    for (int li = 0; li < 1000; li++) {
      final Term result = oncegoal.processGoalOnce();
      final int goalResult = ((TermInteger) result.variables().filter(x -> "Result".equals(x.getText())).findFirst().orElse(null).getValue()).toNumber().intValue();
      if (goalResult < 1 || goalResult > 5) {
        fail("Wrong result, returned value is outbound");
      }
    }

    assertTrue(true);
  }
}
