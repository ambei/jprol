package com.igormaznitsa.prol.test;

import com.igormaznitsa.prol.io.DefaultProlStreamManagerImpl;
import com.igormaznitsa.prol.logic.ChoicePoint;
import com.igormaznitsa.prol.logic.ProlContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class HanoiTowersTest extends AbstractProlTest {

  @Test
  @Disabled("Disabled because pipes have been removed")
  void testHanoiTowers() throws Exception {
    final ProlContext context = new ProlContext("test", DefaultProlStreamManagerImpl.getInstance());
    context.consult(new StringReader("move(1,X,Y,_):-write('Move top disk from '),write(X),write(' to '),write(Y),nl.move(N,X,Y,Z):-N>1,M is N-1,move(M,X,Z,Y),move(1,X,Y,_),move(M,Z,Y,X)."));
    final ChoicePoint goal = new ChoicePoint("tell(\'+hanoi\'),move(3,left,right,center).", context);
    assertNotNull(goal.next());
    assertNull(goal.next());

    String data = "ADD RESULT";

    assertEquals("Move top disk from left to right\nMove top disk from left to center\nMove top disk from right to center\nMove top disk from left to right\nMove top disk from center to left\nMove top disk from center to right\nMove top disk from left to right\n", data);
  }
}
