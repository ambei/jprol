/*
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.igormaznitsa.jprol.libs;

import com.igormaznitsa.jprol.annotations.*;
import com.igormaznitsa.jprol.data.*;
import com.igormaznitsa.jprol.exceptions.*;
import com.igormaznitsa.jprol.kbase.IteratorType;
import com.igormaznitsa.jprol.kbase.KnowledgeBase;
import com.igormaznitsa.jprol.logic.ChoicePoint;
import com.igormaznitsa.jprol.logic.JProlContext;
import com.igormaznitsa.jprol.logic.PredicateInvoker;
import com.igormaznitsa.jprol.logic.triggers.JProlTriggerType;
import com.igormaznitsa.jprol.logic.triggers.JProlTriggeringEventObserver;
import com.igormaznitsa.jprol.utils.Utils;
import com.igormaznitsa.prologparser.tokenizer.OpAssoc;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.igormaznitsa.jprol.data.TermType.*;
import static com.igormaznitsa.jprol.data.Terms.*;
import static com.igormaznitsa.jprol.utils.Utils.TERM_COMPARATOR;
import static com.igormaznitsa.jprol.utils.Utils.createOrAppendToList;
import static com.igormaznitsa.prologparser.tokenizer.OpAssoc.*;

@SuppressWarnings("EmptyMethod")
@ProlOperators(operators = {
    @ProlOperator(priority = 1050, type = XFY, name = "->"),
    @ProlOperator(priority = 900, type = FY, name = "\\+"),
    @ProlOperator(priority = 700, type = XFX, name = ">"),
    @ProlOperator(priority = 700, type = XFX, name = "<"),
    @ProlOperator(priority = 700, type = XFX, name = "=<"),
    @ProlOperator(priority = 700, type = XFX, name = ">="),
    @ProlOperator(priority = 700, type = XFX, name = "=="),
    @ProlOperator(priority = 700, type = XFX, name = "=\\="),
    @ProlOperator(priority = 700, type = XFX, name = "\\=="),
    @ProlOperator(priority = 700, type = XFX, name = "@<"),
    @ProlOperator(priority = 700, type = XFX, name = "@>"),
    @ProlOperator(priority = 700, type = XFX, name = "@=<"),
    @ProlOperator(priority = 700, type = XFX, name = "@>="),
    @ProlOperator(priority = 700, type = XFX, name = "=:="),
    @ProlOperator(priority = 700, type = XFX, name = "=.."),
    @ProlOperator(priority = 500, type = YFX, name = "/\\"),
    @ProlOperator(priority = 500, type = YFX, name = "\\/"),
    @ProlOperator(priority = 500, type = YFX, name = "+"),
    @ProlOperator(priority = 500, type = YFX, name = "-"),
    @ProlOperator(priority = 500, type = FX, name = "+"),
    @ProlOperator(priority = 500, type = FX, name = "-"),
    @ProlOperator(priority = 400, type = YFX, name = "*"),
    @ProlOperator(priority = 400, type = YFX, name = "/"),
    @ProlOperator(priority = 400, type = YFX, name = "//"),
    @ProlOperator(priority = 400, type = YFX, name = "rem"),
    @ProlOperator(priority = 400, type = YFX, name = "<<"),
    @ProlOperator(priority = 400, type = YFX, name = ">>"),
    @ProlOperator(priority = 300, type = XFX, name = "mod"),
    @ProlOperator(priority = 200, type = FY, name = "\\"),
    @ProlOperator(priority = 200, type = XFX, name = "**"),
    @ProlOperator(priority = 200, type = XFY, name = "^")
})
public final class JProlCoreLibrary extends AbstractJProlLibrary {

  public JProlCoreLibrary() {
    super("jprol-core-lib");
  }

  @Predicate(signature = "=:=/2", template = {"@evaluable,@evaluable"}, reference = "Arithmetic Equal")
  @Determined
  public static boolean predicateArithEqu(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.compare(right) == 0;
  }

  @Predicate(signature = "@</2", template = {"?term,?term"}, reference = "Term less than")
  @Determined
  public static boolean predicateTermLess(final ChoicePoint goal, final TermStruct predicate) {
    return predicate.getElement(0).compareTermTo(predicate.getElement(1)) < 0;
  }

  @Predicate(signature = "@=</2", template = {"?term,?term"}, reference = "Term less than or equal to.")
  @Determined
  public static boolean predicateTermLessOrEqu(final ChoicePoint goal, final TermStruct predicate) {
    return predicate.getElement(0).compareTermTo(predicate.getElement(1)) <= 0;
  }

  @Predicate(signature = "@>/2", template = {"?term,?term"}, reference = "Term greater than")
  @Determined
  public static boolean predicateTermMore(final ChoicePoint goal, final TermStruct predicate) {
    return predicate.getElement(0).compareTermTo(predicate.getElement(1)) > 0;
  }

  @Predicate(signature = "@>=/2", template = {"?term,?term"}, reference = "Term greater than or equal to.")
  @Determined
  public static boolean predicateTermMoreOrEqu(final ChoicePoint goal, final TermStruct predicate) {
    return predicate.getElement(0).compareTermTo(predicate.getElement(1)) >= 0;
  }

  @Predicate(signature = "==/2", template = {"?term,?term"}, reference = "Term identical")
  @Determined
  public static boolean predicateTermEqu(final ChoicePoint goal, final TermStruct predicate) {
    return predicate.getElement(0).compareTermTo(predicate.getElement(1)) == 0;
  }

  @Predicate(signature = "\\==/2", template = {"?term,?term"}, reference = "Term not identical")
  @Determined
  public static boolean predicateNotTermEqu(final ChoicePoint goal, final TermStruct predicate) {
    return predicate.getElement(0).compareTermTo(predicate.getElement(1)) != 0;
  }

  @Predicate(signature = ">/2", template = {"@evaluable,@evaluable"}, reference = "Arithmetic greater than")
  @Determined
  public static boolean predicateArithMore(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.compare(right) > 0;
  }

  @Predicate(signature = "</2", template = {"@evaluable,@evaluable"}, reference = "Arithmetic less than")
  @Determined
  public static boolean predicateArithLess(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.compare(right) < 0;
  }

  @Predicate(signature = ">=/2", template = {"@evaluable,@evaluable"}, reference = "Arithmetic greater than or equal to")
  @Determined
  public static boolean predicateArithMoreOrEqu(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.compare(right) >= 0;
  }

  @Predicate(signature = "=</2", template = {"@evaluable,@evaluable"}, reference = "Arithmetic less than or equal to")
  @Determined
  public static boolean predicateArithLessOrEqu(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.compare(right) <= 0;
  }

  @Predicate(signature = "=\\=/2", template = {"@evaluable,@evaluable"}, reference = "Arithmetic Not equal")
  @Determined
  public static boolean predicateArithNotEqu(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.compare(right) != 0;
  }

  @Predicate(signature = "xor/2", template = {"+evaluable,+evaluable"}, reference = "Bitwise exclusive or.")
  @Evaluable
  public static Term predicateXOR(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    if (left instanceof TermLong && right instanceof TermLong) {
      final long lft = left.toNumber().longValue();
      final long rght = right.toNumber().longValue();

      return newLong(lft ^ rght);
    } else {
      throw new ProlTypeErrorException("integer expected", predicate);
    }
  }

  @Predicate(signature = "\\/1", template = {"+evaluable"}, reference = "Bitwise 'not'")
  @Evaluable
  public static Term predicateBITWISENOT(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));

    if (left instanceof TermLong) {
      final long lft = left.toNumber().longValue();

      return newLong(~lft);
    } else {
      throw new ProlTypeErrorException("integer expected", predicate);
    }
  }

  @Predicate(signature = "\\//2", template = {"+evaluable,+evaluable"}, reference = "Bitwise 'or'")
  @Evaluable
  public static Term predicateBITWISEOR(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    if (left instanceof TermLong && right instanceof TermLong) {
      final long lft = left.toNumber().longValue();
      final long rght = right.toNumber().longValue();

      return newLong(lft | rght);
    } else {
      throw new ProlTypeErrorException("integer expected", predicate);
    }
  }

  @Predicate(signature = "/\\/2", template = {"+evaluable,+evaluable"}, reference = "Bitwise 'and'")
  @Evaluable
  public static Term predicateBITWISEAND(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    if (left instanceof TermLong && right instanceof TermLong) {
      final long lft = left.toNumber().longValue();
      final long rght = right.toNumber().longValue();

      return newLong(lft & rght);
    } else {
      throw new ProlTypeErrorException("integer expected", predicate);
    }
  }

  @Predicate(signature = "mod/2", template = {"+evaluable,+evaluable"}, reference = "Modulus")
  @Evaluable
  public static Term predicateMOD(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    if (left instanceof TermLong && right instanceof TermLong) {
      final long rightNum = right.toNumber().longValue();
      if (rightNum == 0L) {
        throw new ProlEvaluationErrorException("zero divisor", predicate);
      }
      return newLong(left.toNumber().longValue() % rightNum);
    } else {
      throw new ProlTypeErrorException("integer expected", predicate);
    }
  }

  @Predicate(signature = "rem/2", template = {"+evaluable,+evaluable"}, reference = "Remainder")
  @Evaluable
  public static Term predicateREM(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    if (left instanceof TermLong && right instanceof TermLong) {
      final long leftNum = left.toNumber().longValue();
      final long rightNum = right.toNumber().longValue();
      if (rightNum == 0L) {
        throw new ProlEvaluationErrorException("zero divisor", predicate);
      }
      return newLong(leftNum - (leftNum / rightNum) * rightNum);
    } else {
      throw new ProlTypeErrorException("integer expected", predicate);
    }
  }

  @Predicate(signature = "**/2", template = {"+evaluable,+evaluable"}, reference = "Power")
  @Evaluable
  public static Term predicatePOWER(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    final double leftval = left.toNumber().doubleValue();
    final double rightval = right.toNumber().doubleValue();

    return newDouble(Math.pow(leftval, rightval));
  }

  @Predicate(signature = "+/2", template = {"+evaluable,+evaluable"}, reference = "Addition")
  @Evaluable
  public static Term predicateADDTWO(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.add(right);
  }

  @Predicate(signature = "sin/1", template = {"+evaluable"}, reference = "Sine")
  @Evaluable
  public static Term predicateSIN(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newDouble(Math.sin(value));
  }

  @Predicate(signature = "float_integer_part/1", template = {"+evaluable"}, reference = "Integer part")
  @Evaluable
  public static Term predicateFLOATINTEGERPART(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final long value = arg.toNumber().longValue();
    return newLong(value);
  }

  @Predicate(signature = "float_fractional_part/1", template = {"+evaluable"}, reference = "Fractional part")
  @Evaluable
  public static Term predicateFLOATFRACTIONALPART(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    final long valueInt = (long) value;
    return newDouble(value - (double) valueInt);
  }

  @Predicate(signature = "floor/1", template = {"+evaluable"}, reference = "Floor")
  @Evaluable
  public static Term predicateFLOOR(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newLong((long) Math.floor(value));
  }

  @Predicate(signature = "truncate/1", template = {"+evaluable"}, reference = "Truncate")
  @Evaluable
  public static Term predicateTRUNCATE(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newLong(value < 0 ? (long) Math.ceil(value) : (long) Math.floor(value));
  }

  @Predicate(signature = "round/1", template = {"+evaluable"}, reference = "Round")
  @Evaluable
  public static Term predicateROUND(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newLong(Math.round(value));
  }

  @Predicate(signature = "ceiling/1", template = {"+evaluable"}, reference = "Ceiling")
  @Evaluable
  public static Term predicateCEILING(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newLong((long) Math.ceil(value));
  }

  @Predicate(signature = "cos/1", template = {"+evaluable"}, reference = "Cosine")
  @Evaluable
  public static Term predicateCOS(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newDouble(Math.cos(value));
  }

  @Predicate(signature = "atan/1", template = {"+evaluable"}, reference = "Arc tangent")
  @Evaluable
  public static Term predicateATAN(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newDouble(Math.atan(value));
  }

  @Predicate(signature = "exp/1", template = {"+evaluable"}, reference = "Exponentiation")
  @Evaluable
  public static Term predicateEXP(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newDouble(Math.exp(value));
  }

  @Predicate(signature = "log/1", template = {"+evaluable"}, reference = "Log")
  @Evaluable
  public static Term predicateLOG(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newDouble(Math.log(value));
  }

  @Predicate(signature = "sqrt/1", template = {"+evaluable"}, reference = "Square root")
  @Evaluable
  public static Term predicateSQRT(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    final double value = arg.toNumber().doubleValue();
    return newDouble(Math.sqrt(value));
  }

  @Predicate(signature = "abs/1", template = {"+evaluable"}, reference = "Absolute value")
  @Evaluable
  public static Term predicateABS(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    return arg.abs();
  }

  @Predicate(signature = "sign/1", template = {"+evaluable"}, reference = "SIGN")
  @Evaluable
  public static Term predicateSIGN(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm arg = calculatEvaluable(goal, predicate.getElement(0));
    return arg.sign();
  }

  @Predicate(signature = "sub_atom/5", template = {"+atom,?integer,?integer,?integer,?atom"}, reference = "Breaking atoms")
  public static boolean predicateSUBATOM(final ChoicePoint goal, final TermStruct predicate) {
    class SubAtomIterator {
      final String atom;
      final int initialLen;
      final String theSub;
      int currentBefore;
      int currentLength;
      int currentAfter;

      private SubAtomIterator(final Term atom, final Term before, final Term length, final Term after, final Term sub) {
        this.atom = atom.getText();
        this.currentBefore = before.getTermType() == VAR ? 0 : before.toNumber().intValue();
        this.currentLength = length.getTermType() == VAR ? 0 : length.toNumber().intValue();
        this.currentAfter = after.getTermType() == VAR ? this.atom.length() : after.toNumber().intValue();
        this.theSub = sub.getTermType() == VAR ? null : sub.getText();

        if (length.getTermType() == VAR) {
          this.initialLen = -1;
        } else {
          this.initialLen = this.currentLength;
        }

        if (before.getTermType() == VAR && after.getTermType() != VAR) {
          this.currentBefore = this.atom.length() - this.currentAfter - this.currentLength;
        }

        if (before.getTermType() != VAR && after.getTermType() == VAR) {
          this.currentAfter = this.atom.length() - this.currentBefore - this.currentLength;
        }

        if (length.getTermType() == VAR) {
          this.currentLength = this.atom.length() - this.currentBefore - this.currentAfter;
        } else if (after.getTermType() == VAR) {
          this.currentAfter = this.atom.length() - this.currentBefore - this.currentLength;
        }
      }

      boolean next(final Term before, final Term length, final Term after, final Term sub) {
        if (this.currentBefore < 0 || this.currentAfter < 0 || this.currentLength < 0) {
          return false;
        }

        final String currentSub = this.atom.substring(this.currentBefore, this.currentBefore + this.currentLength);

        final boolean result = before.unifyTo(Terms.newLong(this.currentBefore))
            && length.unifyTo(Terms.newLong(this.currentLength))
            && after.unifyTo(Terms.newLong(this.currentAfter))
            && sub.unifyTo(Terms.newAtom(currentSub));

        if (this.theSub == null) {
          if (this.initialLen < 0) {
            this.currentLength++;
            this.currentAfter = Math.max(0, this.currentAfter - 1);
            if (this.currentBefore + this.currentLength + this.currentAfter > this.atom.length()) {
              this.currentBefore++;
              this.currentLength = 0;
              this.currentAfter = this.atom.length() - this.currentLength - this.currentBefore;
            }
          } else {
            this.currentBefore++;
            this.currentAfter = this.atom.length() - this.currentLength - this.currentBefore;
          }
        } else {
          this.currentBefore = this.atom.indexOf(this.theSub, this.currentBefore + 1);
          this.currentLength = this.theSub.length();
          this.currentAfter = this.atom.length() - this.currentBefore - this.currentLength;
        }

        return result;
      }
    }

    SubAtomIterator iterator = goal.getPayload();
    if (iterator == null) {
      iterator = new SubAtomIterator(
          predicate.getElement(0).findNonVarOrSame(),
          predicate.getElement(1).findNonVarOrSame(),
          predicate.getElement(2).findNonVarOrSame(),
          predicate.getElement(3).findNonVarOrSame(),
          predicate.getElement(4).findNonVarOrSame()
      );
      goal.setPayload(iterator);
    }

    return iterator.next(
        predicate.getElement(1),
        predicate.getElement(2),
        predicate.getElement(3),
        predicate.getElement(4)
    );
  }

  @Predicate(signature = "-/2", template = {"+evaluable,+evaluable"}, reference = "Subtraction")
  @Evaluable
  public static Term predicateSUBTWO(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.sub(right);
  }

  @Predicate(signature = "-/1", template = {"+evaluable"}, reference = "Negation")
  @Evaluable
  public static Term predicateNeg(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm val = calculatEvaluable(goal, predicate.getElement(0));
    return val.neg();
  }

  @Predicate(signature = "+/1", template = {"+evaluable"}, reference = "Not action over a number")
  @Evaluable
  public static Term predicateTheSame(final ChoicePoint goal, final TermStruct predicate) {
    return calculatEvaluable(goal, predicate.getElement(0));
  }

  @Predicate(signature = "*/2", template = {"+evaluable,+evaluable"}, reference = "Multiplication")
  @Evaluable
  public static Term predicateMUL(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    return left.mul(right);
  }

  @Predicate(signature = "//2", template = {"+evaluable,+evaluable"}, reference = "Division")
  @Evaluable
  public static Term predicateDIV(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    try {
      if ((right instanceof TermDouble && Double.compare(0.0d, right.toNumber().doubleValue()) == 0)
          || (right instanceof TermLong && right.toNumber().longValue() == 0L)) {
        throw new ArithmeticException("Zero divisor");
      }
      return left.div(right);
    } catch (ArithmeticException ex) {
      throw new ProlEvaluationErrorException(ex.getMessage(), predicate, ex);
    }
  }

  @Predicate(signature = "///2", template = {"+evaluable,+evaluable"}, reference = "Integer division.")
  @Evaluable
  public static Term predicateIDIV2(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    if (left instanceof TermDouble || right instanceof TermDouble) {
      throw new ProlTypeErrorException("integer", "Integer expected but float found", predicate);
    }

    try {
      Term result = left.div(right);
      return result;
    } catch (ArithmeticException ex) {
      throw new ProlEvaluationErrorException(ex.getMessage(), predicate, ex);
    }
  }

  @Predicate(signature = "<</2", template = {"+evaluable,+evaluable"}, reference = "Bitwise left shift")
  @Evaluable
  public static Term predicateSHIFTL2(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    final long value = left.toNumber().longValue();
    final long shift = right.toNumber().longValue();

    return newLong(value << shift);
  }

  @Predicate(signature = ">>/2", template = {"+evaluable,+evaluable"}, reference = "Bitwise right shift")
  @Evaluable
  public static Term predicateSHIFTR(final ChoicePoint goal, final TermStruct predicate) {
    final NumericTerm left = calculatEvaluable(goal, predicate.getElement(0));
    final NumericTerm right = calculatEvaluable(goal, predicate.getElement(1));

    final long value = left.toNumber().longValue();
    final long shift = right.toNumber().longValue();

    return newLong(value >> shift);
  }

  @Predicate(signature = "repeat/0", reference = "repeat is true. It just places a choice point every call.")
  public static void predicateREPEAT0(final ChoicePoint goal, final TermStruct predicate) {
    // we just make a choose point
  }

  @Predicate(signature = "clause/2", template = {"+head,?callable_term"}, reference = "clause(Head, Body) is true if and only if\n* The predicate of Head is public (the standard does not specify how a predicate is declared public but dynamic predicates are public, and\n* There is a clause in the database which corresponds to a term H:- B which unifies with Head :- Body.")
  public static boolean predicateCLAUSE2(final ChoicePoint goal, final TermStruct predicate) {
    final Term head = predicate.getElement(0).findNonVarOrSame();
    final Term body = predicate.getElement(1).findNonVarOrSame();

    final TermStruct struct = head.getTermType() == STRUCT ? (TermStruct) head : newStruct(head);
    if (goal.getContext().findProcessor(struct) != PredicateInvoker.NULL_PROCESSOR) {
      throw new ProlPermissionErrorException("access", "private_procedure", predicate);
    }

    Iterator<TermStruct> clIterator = goal.getPayload();

    if (clIterator == null) {
      clIterator = goal.getContext().getKnowledgeBase().iterate(goal.getContext().getKnowledgeContext(), IteratorType.ANY, head.getTermType() == STRUCT ? (TermStruct) head : newStruct(head));
      if (clIterator == null || !clIterator.hasNext()) {
        goal.cutVariants();
        return false;
      }

      goal.setPayload(clIterator);
    }

    TermStruct nxtStruct;
    while (clIterator.hasNext() && (nxtStruct = clIterator.next()) != null) {
      Term headClause;
      Term bodyClause;
      if (nxtStruct.isClause()) {
        headClause = nxtStruct.getElement(0);
        bodyClause = nxtStruct.getElement(1);
      } else {
        headClause = nxtStruct;
        bodyClause = Terms.TRUE;
      }

      if (head.dryUnifyTo(headClause) && body.dryUnifyTo(bodyClause)) {
        final boolean result = assertUnify(head, headClause) && assertUnify(body, bodyClause);
        head.arrangeVariablesInsideTerms(body);
        return result;
      }
    }
    goal.cutVariants();
    return false;
  }

  @Predicate(signature = "current_op/3", template = "?integer,?operator_specifier,?atom", reference = "current_op(Priority, Op_specifier, TermOperator) is true if and only if TermOperator is an operator with properties given by  Op_specifier and Priority")
  @SuppressWarnings("unchecked")
  public static boolean predicateCURRENTOP3(final ChoicePoint goal, final TermStruct predicate) {
    final Term priority = predicate.getElement(0).findNonVarOrSame();
    final Term specifier = predicate.getElement(1).findNonVarOrSame();
    final Term name = predicate.getElement(2).findNonVarOrSame();

    List<Iterator<TermOperator>> list = goal.getPayload();
    if (list == null) {
      list = new ArrayList<>();
      list.add(goal.getContext().getKnowledgeBase().makeOperatorIterator());
      final Iterator<AbstractJProlLibrary> libraries = goal.getContext().makeLibraryIterator();
      while (libraries.hasNext()) {
        list.add(libraries.next().makeOperatorIterator());
      }
      goal.setPayload(list);
    }

    while (!list.isEmpty()) {
      final Iterator<TermOperator> activeIterator = list.get(0);
      while (activeIterator.hasNext()) {
        final TermOperator found = activeIterator.next();
        final Term opPriority = Terms.newLong(found.getPriority());
        final Term opType = Terms.newAtom(found.getTypeAsString());
        final Term opName = Terms.newAtom(found.getText());

        if (priority.dryUnifyTo(opPriority) && specifier.dryUnifyTo(opType) && name.dryUnifyTo(opName)) {
          return assertUnify(priority, opPriority) && assertUnify(specifier, opType) && assertUnify(name, opName);
        }
      }
      list.remove(0);
    }
    goal.cutVariants();
    return false;
  }

  @Predicate(signature = "op/3", template = "+integer,+operator_specifier,@atom_or_atom_list", reference = "Predicate allows to alter operators.\nop(Priority, Op_Specifier, TermOperator) is true, with the side effect that\n1. if Priority is 0 then TermOperator is removed from operators\n2. TermOperator is added into operators, with priority (lower binds tighter) Priority and associativity determined by Op_Specifier")
  @Determined
  public static boolean predicateOP(final ChoicePoint goal, final TermStruct predicate) {
    final int priority = predicate.getElement(0).findNonVarOrSame().toNumber().intValue();
    final String specifier = predicate.getElement(1).findNonVarOrSame().getText();
    final Term atomOrList = predicate.getElement(2).findNonVarOrSame();

    if (priority < 0L || priority > 1200L) {
      throw new ProlDomainErrorException("Priority must be between 0 and 1200 inclusive", predicate);
    }

    OpAssoc opType = OpAssoc.findForName(specifier)
        .orElseThrow(() -> new ProlDomainErrorException("Wrong operator specifier", predicate));

    final ArrayList<String> names = new ArrayList<>();
    if (atomOrList.getTermType() == LIST) {
      TermList list = (TermList) atomOrList;
      while (!list.isNullList()) {
        Term atom = list.getHead();
        if ((atom instanceof NumericTerm) || atom.getTermType() != ATOM) {
          throw new ProlDomainErrorException("Atom expected", predicate);
        }
        names.add(atom.getText());

        atom = list.getTail();
        if (atom.getTermType() != LIST) {
          throw new ProlDomainErrorException("List expected", predicate);
        }
        list = (TermList) atom;
      }
    } else {
      names.add(atomOrList.getText());
    }

    final KnowledgeBase base = goal.getContext().getKnowledgeBase();

    try {
      if (priority == 0) {
        names.forEach((name) -> base.removeOperator(name, opType));
      } else {
        names.forEach((name) -> base.addOperator(goal.getContext(), new TermOperator(priority, opType, name)));
      }
    } catch (SecurityException ex) {
      throw new ProlPermissionErrorException("create", "operator", "Attemption to override or remove a system operator", predicate);
    }
    return true;
  }

  @Predicate(signature = "call/1", template = {"+callable_term"}, reference = "call(G) is true if and only if G represents a goal which is true.")
  public static boolean predicateCALL(final ChoicePoint goal, final TermStruct predicate) {
    final Term argument = predicate.getElement(0).findNonVarOrSame();

    ChoicePoint newChoicePoint = goal.getPayload();
    if (newChoicePoint == null) {
      newChoicePoint = new ChoicePoint(argument, goal.getContext());
      goal.setPayload(newChoicePoint);
    }
    final Term nextResult = newChoicePoint.next();

    boolean result = false;

    if (nextResult != null) {
      result = assertUnify(argument, nextResult);
      if (newChoicePoint.isCompleted()) {
        goal.cutVariants();
      }
    }
    return result;
  }

  @Predicate(signature = "once/1", template = {"+callable_term"}, reference = "once(Term) is true. once/1 is not re-executable.")
  @Determined
  public static boolean predicateONCE(final ChoicePoint goal, final TermStruct predicate) {
    final Term argument = predicate.getElement(0).findNonVarOrSame();
    final ChoicePoint currentgoal = new ChoicePoint(argument, goal.getContext());

    final Term nextResult = currentgoal.next();

    return nextResult != null;
  }

  @Predicate(signature = "->/2", reference = "'->'(If, Then) is true if and only if If is true and Then is true for the first solution of If")
  @ChangesChoosePointChain
  public static boolean predicateIFTHEN(final ChoicePoint goal, final TermStruct predicate) {
    // if-then
    final ChoicePoint leftSubbranch = new ChoicePoint(predicate.getElement(0), goal.getContext());
    boolean result = false;
    if (leftSubbranch.next() != null) {
      // replace current goal by the 'then' goal
      final ChoicePoint thenPart = goal.replaceLastGoalAtChain(predicate.getElement(1));
      thenPart.cutLocally(); // remove all previous choice points
      result = true;
    } else {
      goal.cutVariants();
    }
    return result;
  }

  @Predicate(signature = "var/1", reference = "var(X) is true if and only if X is a variable.")
  @Determined
  public static boolean predicateVAR(final ChoicePoint goal, final TermStruct predicate) {
    return predicate.getElement(0).findNonVarOrSame().getTermType() == VAR;
  }

  @Predicate(signature = "nonvar/1", reference = "nonvar(X) is true if and only if X is not a variable.")
  @Determined
  public static boolean predicateNONVAR(final ChoicePoint goal, final TermStruct predicate) {
    final Term arg = predicate.getElement(0);
    if (arg.getTermType() == VAR) {
      return !((TermVar) arg).isFree();
    } else {
      return true;
    }
  }

  @Predicate(signature = "atom/1", reference = "atom(X) is true if and only if X is an atom.")
  @Determined
  public static boolean predicateATOM(final ChoicePoint goal, final TermStruct predicate) {
    Term arg = predicate.getElement(0);
    if (arg.getTermType() == VAR) {
      arg = ((TermVar) arg).getValue();
      if (arg == null) {
        return false;
      }
    }

    boolean result = false;

    switch (arg.getTermType()) {
      case ATOM: {
        result = !(arg instanceof NumericTerm);
      }
      break;
      case LIST: {
        result = ((TermList) arg).isNullList();
      }
      break;
    }

    return result;
  }

  @Predicate(signature = "integer/1", reference = "integer(X) is true if and only if X is an integer.")
  @Determined
  public static boolean predicateINTEGER(final ChoicePoint goal, final TermStruct predicate) {
    final Term arg = predicate.getElement(0).findNonVarOrSame();

    if (arg.getTermType() == ATOM) {
      return arg instanceof TermLong;
    } else {
      return false;
    }
  }

  @Predicate(signature = "number/1", reference = "number(X) is true if and only if X is an integer or a float.")
  @Determined
  public static boolean predicateNUMBER(final ChoicePoint goal, final TermStruct predicate) {
    final Term arg = predicate.getElement(0).findNonVarOrSame();
    return (arg.getTermType() == ATOM) && (arg instanceof NumericTerm);
  }

  @Predicate(signature = "float/1", reference = "float(X) is true if and only if X is a float.")
  @Determined
  public static boolean predicateFLOAT(final ChoicePoint goal, final TermStruct predicate) {
    final Term arg = predicate.getElement(0).findNonVarOrSame();
    if (arg.getTermType() == ATOM) {
      return arg instanceof TermDouble;
    } else {
      return false;
    }
  }

  @Predicate(signature = "compound/1", reference = "compound(X) is true if and only if X is a compound term, that is neither atomic nor a variable.")
  @Determined
  public static boolean predicateCOMPOUND(final ChoicePoint goal, final TermStruct predicate) {
    final Term atom = predicate.getElement(0).findNonVarOrSame();
    switch (atom.getTermType()) {
      case STRUCT:
        return true;
      case LIST:
        return !((TermList) atom).isNullList();
      default:
        return false;
    }
  }

  @Predicate(signature = "atomic/1", reference = "atomic(X) is true if and only if X is atomic (that is an atom, an integer or a float).")
  @Determined
  public static boolean predicateATOMIC(final ChoicePoint goal, final TermStruct predicate) {
    final Term arg = predicate.getElement(0).findNonVarOrSame();
    boolean result = false;
    switch (arg.getTermType()) {
      case ATOM: {
        result = true;
      }
      break;
      case LIST: {
        result = ((TermList) arg).isNullList();
      }
      break;
    }
    return result;
  }

  @Predicate(signature = "arg/3", template = {"+integer,+compound_term,?term"}, reference = "arg(N,Term, Arg) is true if nad only if the Nth argument of Term is Arg")
  @Determined
  public static boolean predicateARG(final ChoicePoint goal, final TermStruct predicate) {
    final TermLong number = predicate.getElement(0).findNonVarOrSame();
    final Term compound_term = predicate.getElement(1).findNonVarOrSame();
    final Term element = predicate.getElement(2).findNonVarOrSame();

    final long index = number.toNumber().longValue();

    if (index < 0) {
      throw new ProlDomainErrorException("Element number less than zero", number);
    }
    if (index == 0) {
      return false;
    }

    boolean result = false;
    if (compound_term.getTermType() == STRUCT) {
      final TermStruct struct = (TermStruct) compound_term;
      final long elementIndex = index - 1;
      if (elementIndex < struct.getArity()) {
        result = element.unifyTo(struct.getElement((int) elementIndex));
      }
    }
    return result;
  }

  @Predicate(signature = "functor/3", template = {"-nonvar,+atomic,+integer", "+nonvar,?atomic,?integer"}, reference = "functor(Term, Name, Arity) is true if and only if Term is a compound term with functor name Name and arity Arity or Term is an atomic term equal to Name and Arity is 0.")
  @Determined
  public static boolean predicateFUNCTOR(final ChoicePoint goal, final TermStruct predicate) {
    final Term argTerm = predicate.getElement(0).findNonVarOrSame();
    final Term argName = predicate.getElement(1).findNonVarOrSame();
    final Term argArity = predicate.getElement(2).findNonVarOrSame();

    switch (argTerm.getTermType()) {
      case ATOM: {
        final Term arity = newLong(0);
        return argName.unifyTo(argTerm) && argArity.unifyTo(arity);
      }
      case STRUCT: {
        final TermStruct struct = (TermStruct) argTerm;
        final Term functor = newAtom(struct.getFunctor().getText());
        final Term arity = newLong(struct.getArity());
        return argName.unifyTo(functor) && argArity.unifyTo(arity);
      }
      case LIST: {
        final TermList list = (TermList) argTerm;

        Term name;
        Term arity;

        if (list.isNullList()) {
          arity = newLong(0);
          name = NULL_LIST;
        } else {
          arity = newLong(2);
          name = LIST_FUNCTOR;
        }
        return argName.unifyTo(name) && argArity.unifyTo(arity);
      }
      case VAR: {
        final int arity = (int) argArity.toNumber().longValue();
        if (arity < 0) {
          throw new ProlRepresentationErrorException("Wrong arity value", predicate);
        }

        if (argName instanceof NumericTerm) {
          if (arity == 0) {
            return argTerm.unifyTo(argName);
          } else {
            throw new ProlTypeErrorException("atom", predicate);
          }
        }

        Term[] elements = null;

        if (arity > 0) {
          elements = new Term[arity];
          for (int li = 0; li < arity; li++) {
            elements[li] = newVar();
          }
        }

        final TermStruct newStruct = newStruct(argName, elements);

        return argTerm.unifyTo(newStruct);
      }
      default:
        throw new ProlCriticalError("Unexpected type:" + argTerm.getTermType());
    }

  }

  @Predicate(signature = "=../2", template = {"+nonvar,?non_empty_list", "-nonvar,+non_empty_list"}, reference = "Term =.. List is true if and only if\n* Term is an atomic term and List is the list whose only element is Term, or\n* Term is a compound term and List is the list whose head is the functor name of Term and whose tail is the list of the arguments of Term. ")
  @Determined
  public static boolean predicateUNIV(final ChoicePoint goal, final TermStruct predicate) {
    final Term argLeft = predicate.getElement(0).findNonVarOrSame();
    final Term argRight = predicate.getElement(1).findNonVarOrSame();

    if (argLeft.getTermType() == STRUCT) {
      if (((TermStruct) argLeft).getArity() == 0) {
        throw new ProlDomainErrorException("compound_non_zero_arity", predicate);
      }
    }

    if (argRight.getTermType() == VAR) {
      TermList list = TermList.asTermList(argLeft);
      return argRight.unifyTo(list);
    } else {
      final Term atom = ((TermList) argRight).toAtom();
      if (atom.getTermType() == STRUCT) {
        final TermStruct atomAsStruct = (TermStruct) atom;
        atomAsStruct.setPredicateProcessor(goal.getContext().findProcessor(atomAsStruct));
      }
      return argLeft.unifyTo(atom);
    }
  }

  @Predicate(signature = "atom_chars/2", template = {"+atom,?list", "-atom,+character_list"}, reference = "atom_chars(Atom, List) succeeds if and only if List is a list whose elements are the one character atoms that in order make up  Atom.")
  @Determined
  public static boolean predicateATOMCHARS(final ChoicePoint goal, final TermStruct predicate) {
    Term left = predicate.getElement(0).findNonVarOrSame();
    Term right = predicate.getElement(1).findNonVarOrSame();

    switch (left.getTermType()) {
      case ATOM: {
        left = left.toCharList();
        return left.unifyTo(right);
      }
      case LIST: {
        if (((TermList) left).isNullList()) {
          left = newAtom("[]").toCharList();
          return left.unifyTo(right);
        } else {
          throw new ProlTypeErrorException("atom", predicate);
        }
      }
    }

    if (right.getTermType() == LIST) {
      StringBuilder builder = new StringBuilder();

      TermList list = (TermList) right;
      while (!list.isNullList()) {
        final Term head = list.getHead();
        builder.append(head.getText());

        final Term tail = list.getTail();
        if (tail.getTermType() == LIST) {
          list = (TermList) tail;
        } else {
          return false;
        }
      }

      right = newAtom(builder.toString());
      return left.unifyTo(right);
    }

    return false;
  }

  @Predicate(signature = "char_code/2", template = {"+character,?character_code", "-character,+character_code"}, reference = "char_code(Char, Code) succeeds if and only if Code is the character code that corresponds to the character Char.")
  @Determined
  public static boolean predicateCHARCODE(final ChoicePoint goal, final TermStruct predicate) {
    Term left = predicate.getElement(0).findNonVarOrSame();
    Term right = predicate.getElement(1).findNonVarOrSame();

    if (left.getTermType() == ATOM) {
      left = newLong((int) left.getText().charAt(0));
      return right.unifyTo(left);
    }

    if (right.getTermType() == ATOM) {
      right = newAtom(Character.toString((char) right.toNumber().shortValue()));
      return left.unifyTo(right);
    }

    return false;
  }

  @Predicate(signature = "number_codes/2", template = {"+number,?character_code_list", "-number,+character_code_list"}, reference = "number_codes(Number, CodeList) succeeds if and only if CodeList is a list whose elements are the codes for the one character atoms that in order make up Number.")
  @Determined
  public static boolean predicateNUMBERCODES(final ChoicePoint goal, final TermStruct predicate) {
    Term left = predicate.getElement(0).findNonVarOrSame();
    final Term right = predicate.getElement(1).findNonVarOrSame();

    if (left.getTermType() == ATOM && right.getTermType() == VAR) {
      left = left.toCharCodeList();
      return left.unifyTo(right);
    }

    if (right.getTermType() == LIST) {
      final StringBuilder builder = new StringBuilder();

      TermList list = (TermList) right;
      while (!list.isNullList()) {
        final TermLong head = list.getHead();
        builder.append((char) head.toNumber().shortValue());

        final Term tail = list.getTail();
        if (tail.getTermType() == LIST) {
          list = (TermList) tail;
        } else {
          return false;
        }
      }

      final String numberValue = builder.toString();

      Term number;

      try {
        if (numberValue.startsWith("0x")) {
          number = newLong(Long.parseLong(numberValue.substring(2), 16));
        } else {
          number = newLong(numberValue);
        }
      } catch (NumberFormatException ex) {
        try {
          number = newDouble(numberValue);
        } catch (NumberFormatException exx) {
          number = newAtom(numberValue);
        }
      }

      return left.unifyTo(number);
    }

    return false;
  }

  @Predicate(signature = "current_predicate_all/1",
      template = {"?predicate_indicator"},
      reference = "True if PredicateIndicator is a currently defined predicate. It looks for predicates both in knowledge base and attached libraries."
  )
  public static boolean predicateCURRENTPREDICATEALL(final ChoicePoint goal, final TermStruct predicate) {
    final Term predicateIndicator = predicate.getElement(0).findNonVarOrSame();
    List<TermStruct> list = goal.getPayload();
    if (list == null) {
      list = new ArrayList<>(goal.getContext().findAllForPredicateIndicatorInLibs(predicateIndicator));

      final Iterator<TermStruct> iter = goal.getContext().getKnowledgeBase().iterateSignatures(goal.getContext().getKnowledgeContext(), predicateIndicator.getTermType() == VAR ? Terms.newStruct("/", new Term[] {Terms.newVar(), Terms.newVar()}) : (TermStruct) predicateIndicator);
      while (iter.hasNext()) {
        list.add(iter.next());
      }
      list.sort(TermStruct::compareTermTo);
      goal.setPayload(list);
    }

    if (list.isEmpty()) {
      return false;
    } else {
      return predicateIndicator.unifyTo(list.remove(0));
    }
  }

  @Predicate(signature = "current_predicate/1",
      template = {"?predicate_indicator"},
      reference = "True if PredicateIndicator is a currently defined predicate. It looks for predicates only in current knowledge base."
  )
  public static boolean predicateCURRENTPREDICATE(final ChoicePoint goal, final TermStruct predicate) {
    final Term predicateIndicator = predicate.getElement(0).findNonVarOrSame();
    List<TermStruct> list = goal.getPayload();
    if (list == null) {
      list = new ArrayList<>();
      final Iterator<TermStruct> iter = goal.getContext().getKnowledgeBase().iterateSignatures(goal.getContext().getKnowledgeContext(), predicateIndicator.getTermType() == VAR ? Terms.newStruct("/", new Term[] {Terms.newVar(), Terms.newVar()}) : (TermStruct) predicateIndicator);
      while (iter.hasNext()) {
        list.add(iter.next());
      }
      list.sort(TermStruct::compareTermTo);
      goal.setPayload(list);
    }

    if (list.isEmpty()) {
      return false;
    } else {
      return predicateIndicator.unifyTo(list.remove(0));
    }
  }

  @Predicate(signature = "atom_concat/3",
      template = {"?atom,?atom,?atom"},
      reference = "Atom3 forms the concatenation of Atom1 and Atom2.")
  public static boolean predicateATOMCONCAT3(final ChoicePoint goal, final TermStruct predicate) {
    final Term atom1 = predicate.getElement(0).findNonVarOrSame();
    final Term atom2 = predicate.getElement(1).findNonVarOrSame();
    final Term atom3 = predicate.getElement(2).findNonVarOrSame();

    final int bounded = (atom1.isGround() ? 1 : 0) + (atom2.isGround() ? 1 : 0) + (atom3.isGround() ? 2 : 0);

    class AtomConcatState {
      private final StringBuilder seq1;
      private final StringBuilder seq2;

      AtomConcatState(final String text2) {
        this.seq1 = new StringBuilder(text2.length());
        this.seq2 = new StringBuilder(text2);
      }

      boolean next() {
        boolean result = false;
        if (this.seq2.length() > 0) {
          this.seq1.append(this.seq2.charAt(0));
          this.seq2.delete(0, 1);
          result = true;
        }
        return result;
      }

      Term getSeq1AsTerm() {
        return newAtom(this.seq1.toString());
      }

      Term getSeq2AsTerm() {
        return newAtom(this.seq2.toString());
      }
    }

    if (bounded < 2) {
      throw new ProlInstantiationErrorException(predicate);
    } else {
      final AtomConcatState state = goal.getPayload();
      if (state == null) {
        if (atom1.isGround() && atom2.isGround()) {
          goal.cutVariants();
          return atom3.unifyTo(newAtom(atom1.getText() + atom2.getText()));
        } else if (atom1.isGround()) {
          goal.cutVariants();
          final String text1 = atom1.getText();
          final String text3 = atom3.getText();
          if (text3.startsWith(text1)) {
            return atom2.unifyTo(newAtom(text3.substring(text1.length())));
          }
        } else if (atom2.isGround()) {
          goal.cutVariants();
          final String text2 = atom2.getText();
          final String text3 = atom3.getText();
          if (text3.endsWith(text2)) {
            return atom1.unifyTo(newAtom(text3.substring(0, text3.length() - text2.length())));
          }
        } else {
          final String wholeText = atom3.getText();
          final AtomConcatState newState = new AtomConcatState(wholeText);
          goal.setPayload(newState);
          return atom1.unifyTo(newState.getSeq1AsTerm()) && atom2.unifyTo(newState.getSeq2AsTerm());
        }
      } else {
        boolean result = state.next();
        if (result) {
          result = atom1.unifyTo(state.getSeq1AsTerm()) && atom2.unifyTo(state.getSeq2AsTerm());
        } else {
          goal.cutVariants();
        }
        return result;
      }
    }
    return false;
  }


  @Predicate(signature = "number_chars/2",
      template = {"+number,?character_list", "-number,+character_list"},
      reference = "number_chars(Number, List) succeeds if and only if List is a list whose elements are the one character atoms that in order make up Number.")
  @Determined
  public static boolean predicateNUMBERCHARS2(final ChoicePoint goal, final TermStruct predicate) {
    Term left = predicate.getElement(0).findNonVarOrSame();
    final Term right = predicate.getElement(1).findNonVarOrSame();

    if (right.getTermType() == LIST) {
      final StringBuilder builder = new StringBuilder();

      TermList list = (TermList) right;
      boolean add = false;
      while (!list.isNullList()) {
        final Term head = list.getHead();

        final char chr = head.getText().charAt(0);
        if (!add) {
          if (!Character.isWhitespace(chr)) {
            add = true;
            builder.append(chr);
          }
        } else {
          builder.append(chr);
        }

        final Term tail = list.getTail();
        if (tail.getTermType() == LIST) {
          list = (TermList) tail;
        } else {
          return false;
        }
      }

      Term number;

      final String numberValue = builder.toString();

      try {
        if (numberValue.startsWith("0x")) {
          number = newLong(Long.parseLong(numberValue.substring(2), 16));
        } else {
          number = newLong(numberValue);
        }
      } catch (NumberFormatException ex) {
        try {
          number = newDouble(numberValue);
        } catch (NumberFormatException exx) {
          throw new ProlCustomErrorException(newStruct(newAtom("syntax_error"), new Term[] {newAtom(numberValue)}), predicate);
        }
      }

      return left.unifyTo(number);
    }

    if (left.getTermType() == ATOM) {
      left = left.toCharList();
      return left.unifyTo(right);
    }

    return false;
  }

  @Predicate(signature = "for/3", template = {"?term,+integer,+integer"}, reference = "Allows to make integer counter from a variable, (TermVar, Low, High).")
  public static boolean predicateFOR3(final ChoicePoint goal, final TermStruct predicate) {
    final Term term = predicate.getElement(0).findNonVarOrSame();
    final long low = predicate.getElement(1).findNonVarOrSame().toNumber().longValue();
    final long high = predicate.getElement(2).findNonVarOrSame().toNumber().longValue();
    AtomicLong counter = goal.getPayload();
    if (counter == null) {
      counter = new AtomicLong(low);
      goal.setPayload(counter);
    } else {
      counter.incrementAndGet();
    }
    final long value = counter.longValue();
    if (value > high) {
      goal.cutVariants();
      return false;
    }
    return term.unifyTo(Terms.newLong(value));
  }

  @Predicate(signature = "rnd/2", template = {"+integer,?integer", "+list,?term"}, reference = "Generate pseudo random in 0(included)...limit(excluded) or select random element from the list.")
  @Determined
  public static boolean predicateRND(final ChoicePoint goal, final TermStruct predicate) {
    final Term first = predicate.getElement(0).findNonVarOrSame();
    final Term second = predicate.getElement(1).findNonVarOrSame();

    final Term result;
    if (first.getTermType() == LIST) {
      final TermList list = (TermList) first;
      if (list.isNullList()) {
        result = Terms.NULL_LIST;
      } else {
        final Term[] array = list.toArray();
        result = array[ThreadLocalRandom.current().nextInt(array.length)];
      }
    } else {
      result = Terms.newLong(ThreadLocalRandom.current().nextLong(first.toNumber().longValue()));
    }
    return second.unifyTo(result);
  }

  @Predicate(signature = "atom_length/2", template = {"+atom,?integer"}, reference = "atom_length(Atom, Length) is true if and only if the integer Length equals the number of characters in the name of the atom Atom.")
  @Determined
  public static boolean predicateATOMLENGTH(final ChoicePoint goal, final TermStruct predicate) {
    Term left = predicate.getElement(0).findNonVarOrSame();
    final Term right = predicate.getElement(1).findNonVarOrSame();

    left = newLong(left.getTextLength());
    return left.unifyTo(right);
  }

  @Predicate(signature = "atom_codes/2", template = {"+atom,?character_code_list", "?atom,+list"}, reference = "atom_codes(Atom, List) succeeds if and only if List is a list whose elements are the character codes that in order correspond to the characters that make up  Atom.")
  @Determined
  public static boolean predicateATOMCHARCODES(final ChoicePoint goal, final TermStruct predicate) {
    Term left = predicate.getElement(0).findNonVarOrSame();
    Term right = predicate.getElement(1).findNonVarOrSame();

    switch (left.getTermType()) {
      case ATOM: {
        left = left.toCharCodeList();
        return left.unifyTo(right);
      }
      case LIST: {
        if (((TermList) left).isNullList()) {
          left = newAtom("[]").toCharCodeList();
          return left.unifyTo(right);
        } else {
          throw new ProlTypeErrorException("atom", predicate);
        }
      }
    }

    if (left.getTermType() == ATOM) {
      left = left.toCharCodeList();

      return left.unifyTo(right);
    }

    if (right.getTermType() == LIST) {
      final StringBuilder builder = new StringBuilder();

      TermList list = (TermList) right;
      while (!list.isNullList()) {
        final Term head = list.getHead();

        if (!(head instanceof TermLong)) {
          throw new ProlRepresentationErrorException("character_code", predicate);
        }

        builder.append((char) head.toNumber().shortValue());

        final Term tail = list.getTail();
        if (tail.getTermType() == LIST) {
          list = (TermList) tail;
        } else {
          return false;
        }
      }

      right = newAtom(builder.toString());
      return left.unifyTo(right);
    }

    return false;
  }

  @Predicate(signature = "dispose/1", template = {"+integer"}, reference = " These predicate terminate a Prolog engine and you can send the status of a cause.")
  @PredicateSynonyms(signatures = {"dispose/0"})
  @Determined
  public static void predicateHALT(final ChoicePoint goal, final TermStruct predicate) {
    if (predicate.getArity() == 0) {
      goal.getContext().dispose();
      throw new ProlHaltExecutionException();
    } else {
      final long status = predicate.getElement(0).findNonVarOrSame().toNumber().longValue();
      goal.getContext().dispose();
      throw new ProlHaltExecutionException(status);
    }
  }

  @Predicate(signature = "abolish/1", template = {"@predicate_indicator"}, reference = "abolish(Pred/2) is true. It has for side effect the removal of all clauses of the predicate indicated by Pred. After abolish/1 the predicate is not found by current_predicate.")
  @Determined
  public static boolean predicateABOLISH(final ChoicePoint goal, final TermStruct predicate) {
    final String signature = Utils.extractPredicateSignatureFromStructure(predicate.getElement(0));
    final KnowledgeBase base = goal.getContext().getKnowledgeBase();

    if (goal.getContext().hasPredicateAtLibraryForSignature(signature)) {
      throw new ProlPermissionErrorException("modify", "static_procedure", newAtom(signature));
    }

    base.abolish(goal.getContext(), signature);
    return true;
  }

  @Predicate(signature = "sort/2", template = {"+list,?list"}, reference = "True if Sorted can be unified with a list holding the elements of List, sorted to the standard order of terms")
  @Determined
  public static boolean predicateSORT2(final ChoicePoint goal, final TermStruct predicate) {
    final Term termList = predicate.getElement(0).findNonVarOrSame();
    final Term termSorted = predicate.getElement(1).findNonVarOrSame();

    if (termSorted.getTermType() == VAR) {
      final Term[] terms = ((TermList) termList).toArray();
      Arrays.sort(terms, TERM_COMPARATOR);
      final TermList sortedList;
      if (terms.length > 1) {
        for (int i = terms.length - 1; i > 0; i--) {
          final Term term = terms[i];
          final Term termPrev = terms[i - 1];
          if (TERM_COMPARATOR.compare(term, termPrev) == 0) {
            terms[i] = null;
          }
        }
        sortedList = TermList.asTermList(Arrays.stream(terms).filter(Objects::nonNull).toArray(Term[]::new));
      } else {
        sortedList = TermList.asTermList(terms);
      }
      return termSorted.unifyTo(sortedList);
    } else {
      return termList.unifyTo(termSorted);
    }
  }

  @Predicate(signature = "findall/3", template = {"?term,+callable_term,?list"}, reference = "Creates  a list of the instantiations Template gets  successively on backtracking  over Goal and unifies the  result with Bag.")
  @Determined
  public static boolean predicateFINDALL3(final ChoicePoint goal, final TermStruct predicate) {
    final Term template = predicate.getElement(0).findNonVarOrSame();
    final Term pgoal = predicate.getElement(1).findNonVarOrSame();
    final Term instances = predicate.getElement(2).findNonVarOrSame();

    final ChoicePoint find_goal = new ChoicePoint(pgoal.makeClone(), goal.getContext());

    TermList result = null;
    TermList currentList = null;

    while (true) {
      final Term nextTemplate = find_goal.next();

      if (nextTemplate == null) {
        break;
      }

      final Term templateCopy = template.makeClone();
      final Term pgoalCopy = pgoal.makeClone();
      templateCopy.arrangeVariablesInsideTerms(pgoalCopy);

      assertUnify(pgoalCopy, nextTemplate);
        // good, add to the list
        if (result == null) {
          // first
          result = newList(templateCopy.findNonVarOrSame().makeClone());
          currentList = result;
        } else {
          // not first
          currentList = createOrAppendToList(currentList, templateCopy.findNonVarOrSame().makeClone());
        }
    }

    if (result == null) {
      result = NULL_LIST;
    }

    return instances.unifyTo(result);
  }

  @Predicate(signature = "bagof/3", template = {"?term,+callable_term,?list"}, reference = "Unify Bag with the alternatives of Template. If Goal has free variables besides the one sharing with Template, bagof/3 will backtrack over the alternatives of these free variables, unifying Bag with the corresponding alternatives of Template. The construct +TermVar^Goal tells bagof/3 not to bind TermVar in Goal. bagof/3 fails if Goal has no solutions.")
  public static boolean predicateBAGOF(final ChoicePoint goal, final TermStruct predicate) {

    final class BofKey {

      private final Map<String, Term> vars;
      private final int hash;

      BofKey(final ChoicePoint goal, final Set<String> excludedVariables) {
        final Map<String, Term> varSnapshot = goal.findAllGroundedVars();
        excludedVariables.forEach(varSnapshot::remove);
        final List<String> orderedNames = new ArrayList<>(varSnapshot.keySet());
        Collections.sort(orderedNames);
        this.hash = orderedNames.stream().map(n -> varSnapshot.get(n).getText()).collect(Collectors.joining(":")).hashCode();
        this.vars = varSnapshot;
      }

      public void restoreVarValues(final ChoicePoint goal) {
        this.vars.keySet().forEach(name -> {
          final TermVar thatvar = goal.getVarForName(name);
          if (thatvar != null) {
            thatvar.unifyTo(this.vars.get(name));
          }
        });
      }

      @Override
      public int hashCode() {
        return this.hash;
      }

      @Override
      public boolean equals(final Object that) {
        if (that == this) {
          return true;
        }
        boolean result = false;

        if (that instanceof BofKey && ((BofKey) that).vars.size() == this.vars.size()) {
          final BofKey thatKey = (BofKey) that;
          result = this.vars.entrySet().stream()
              .allMatch(e -> thatKey.vars.containsKey(e.getKey()) && thatKey.vars.get(e.getKey()).dryUnifyTo(e.getValue()));
        }
        return result;
      }

    }

    final Term template = predicate.getElement(0).findNonVarOrSame();
    final Term pgoal = predicate.getElement(1).findNonVarOrSame();
    final Term instances = predicate.getElement(2).findNonVarOrSame();

    Map<BofKey, TermList> preparedMap = goal.getPayload();

    if (preparedMap == null) {
      preparedMap = new LinkedHashMap<>();

      final Set<String> excludedVars = new HashSet<>(template.allNamedVarsAsMap().keySet());

      Term processingGoal = pgoal;
      while (processingGoal.getTermType() == STRUCT
          && ((TermStruct) processingGoal).getArity() == 2
          && "^".equals(((TermStruct) processingGoal).getFunctor().getText())) {

        final TermStruct theStruct = (TermStruct) processingGoal;
        final Term left = theStruct.getElement(0);

        if (left.getTermType() == VAR) {
          excludedVars.add(left.getText());
        } else {
          throw new ProlTypeErrorException("var", "Expected VAR as left side argument", left);
        }

        processingGoal = theStruct.getElement(1);
      }

      final ChoicePoint find_goal = new ChoicePoint(processingGoal.makeClone(), goal.getContext());

      while (true) {
        final Term nextTemplate = find_goal.next();

        if (nextTemplate == null) {
          break;
        }

        final Term templateCopy = template.makeClone();
        final Term pgoalCopy = processingGoal.makeClone();
        templateCopy.arrangeVariablesInsideTerms(pgoalCopy);

        assertUnify(pgoalCopy, nextTemplate);
          final BofKey thekey = new BofKey(find_goal, excludedVars);
          final TermList resultList;
          if (preparedMap.containsKey(thekey)) {
            resultList = preparedMap.get(thekey);
            createOrAppendToList(resultList, templateCopy.findNonVarOrSame().makeClone());
          } else {
            resultList = newList(templateCopy.findNonVarOrSame().makeClone());
            preparedMap.put(thekey, resultList);
          }
      }

      goal.setPayload(preparedMap);
    }

    if (preparedMap.isEmpty()) {
      return false;
    } else {
      final BofKey firstKey = preparedMap.keySet().stream().findFirst().get();
      final TermList list = preparedMap.remove(firstKey);
      if (instances.unifyTo(list)) {
        firstKey.restoreVarValues(goal);
        return true;
      } else {
        return false;
      }
    }
  }

  @Predicate(signature = "setof/3", template = {"?term,+callable_term,?list"}, reference = "Equivalent to bagof/3, but sorts the result using sort/2 to get a sorted list of alternatives without duplicates.")
  public static boolean predicateSETOF3(final ChoicePoint goal, final TermStruct predicate) {

    final class SofKey {

      private final Map<String, Term> vars;
      private final int hash;

      SofKey(final ChoicePoint goal, final Set<String> excludedVariables) {
        final Map<String, Term> varSnapshot = goal.findAllGroundedVars();
        excludedVariables.forEach(varSnapshot::remove);
        final List<String> orderedNames = new ArrayList<>(varSnapshot.keySet());
        Collections.sort(orderedNames);
        this.hash = orderedNames.stream().map(n -> varSnapshot.get(n).getText()).collect(Collectors.joining(":")).hashCode();
        this.vars = varSnapshot;
      }

      public void restoreVarValues(final ChoicePoint goal) {
        this.vars.keySet().forEach(name -> {
          final TermVar thatvar = goal.getVarForName(name);
          if (thatvar != null) {
            thatvar.unifyTo(this.vars.get(name));
          }
        });
      }

      @Override
      public int hashCode() {
        return this.hash;
      }

      @Override
      public boolean equals(final Object that) {
        if (that == this) {
          return true;
        }
        boolean result = false;

        if (that instanceof SofKey && ((SofKey) that).vars.size() == this.vars.size()) {
          final SofKey thatKey = (SofKey) that;
          result = this.vars.entrySet().stream()
              .allMatch(e -> thatKey.vars.containsKey(e.getKey()) && thatKey.vars.get(e.getKey()).dryUnifyTo(e.getValue()));
        }
        return result;
      }

    }

    final Term template = predicate.getElement(0).findNonVarOrSame();
    final Term pgoal = predicate.getElement(1).findNonVarOrSame();
    final Term instances = predicate.getElement(2).findNonVarOrSame();

    Map<SofKey, TermList> preparedMap = goal.getPayload();

    if (preparedMap == null) {
      preparedMap = new LinkedHashMap<>();

      final Set<String> excludedVars = new HashSet<>(template.allNamedVarsAsMap().keySet());

      Term processingGoal = pgoal;
      while (processingGoal.getTermType() == STRUCT
          && ((TermStruct) processingGoal).getArity() == 2
          && "^".equals(((TermStruct) processingGoal).getFunctor().getText())) {

        final TermStruct theStruct = (TermStruct) processingGoal;
        final Term left = theStruct.getElement(0);

        if (left.getTermType() == VAR) {
          excludedVars.add(left.getText());
        } else {
          throw new ProlTypeErrorException("var", "Expected VAR as left side argument", left);
        }

        processingGoal = theStruct.getElement(1);
      }

      final ChoicePoint find_goal = new ChoicePoint(processingGoal.makeClone(), goal.getContext());

      while (true) {
        final Term nextTemplate = find_goal.next();

        if (nextTemplate == null) {
          break;
        }

        final Term templateCopy = template.makeClone();
        final Term pgoalCopy = processingGoal.makeClone();
        templateCopy.arrangeVariablesInsideTerms(pgoalCopy);

        assertUnify(pgoalCopy, nextTemplate);
          final SofKey thekey = new SofKey(find_goal, excludedVars);
          final TermList resultList;
          if (preparedMap.containsKey(thekey)) {
            resultList = preparedMap.get(thekey);
            Utils.createOrAppendToList(resultList, templateCopy.findNonVarOrSame().makeClone());
          } else {
            resultList = newList(templateCopy.findNonVarOrSame().makeClone());
            preparedMap.put(thekey, resultList);
          }
      }

      final Map<SofKey, TermList> sortedMap = new LinkedHashMap<>();
      preparedMap.forEach((key, value) -> {
        final Term[] tmpArray = value.toArray();
        Arrays.sort(tmpArray, Utils.TERM_COMPARATOR);
        final TermList sortedList = TermList.asTermList(
            Arrays.stream(tmpArray)
                .distinct().toArray(Term[]::new)
        );

        sortedMap.put(key, sortedList);
      });

      preparedMap = sortedMap;

      goal.setPayload(preparedMap);
    }

    if (preparedMap.isEmpty()) {
      return false;
    } else {
      final SofKey firstKey = preparedMap.keySet().stream().findFirst().get();
      final TermList list = preparedMap.remove(firstKey);
      if (instances.unifyTo(list)) {
        firstKey.restoreVarValues(goal);
        return true;
      } else {
        return false;
      }
    }
  }

  @Predicate(signature = "asserta/1", template = {"@clause"}, reference = "Addition of a clause into the knowlwde base before all other clauses.")
  @Determined
  public static boolean predicateASSERTA1(final ChoicePoint goal, final TermStruct predicate) {
    final KnowledgeBase base = goal.getContext().getKnowledgeBase();

    Term termToAdd = predicate.getElement(0).findNonVarOrSame();

    if (termToAdd.getTermType() != STRUCT) {
      termToAdd = newStruct(termToAdd);
    }

    final String signature = ((TermStruct) termToAdd).isClause() ? ((TermStruct) termToAdd).getElement(0).getSignature() : termToAdd.getSignature();

    // check that we doesn't overload any static system predicate
    if (goal.getContext().hasPredicateAtLibraryForSignature(signature)) {
      throw new ProlPermissionErrorException("modify", "static_procedure", newAtom(signature));
    }

    base.assertA(goal.getContext(), (TermStruct) termToAdd.makeCloneAndVarBound());
    return true;
  }

  @Predicate(signature = "assertz/1", template = {"@clause"}, reference = "Addition of a clause into the knowlwde base after all other clauses.")
  @PredicateSynonyms(signatures = "assert/1")
  @Determined
  public static boolean predicateASSERTZ1(final ChoicePoint goal, final TermStruct predicate) {
    final KnowledgeBase base = goal.getContext().getKnowledgeBase();
    Term termToRemove = predicate.getElement(0).findNonVarOrSame();

    if (termToRemove.getTermType() != STRUCT) {
      termToRemove = newStruct(termToRemove);
    }

    final String signature = ((TermStruct) termToRemove).isClause() ? ((TermStruct) termToRemove).getElement(0).getSignature() : termToRemove.getSignature();

    if (goal.getContext().hasPredicateAtLibraryForSignature(signature)) {
      throw new ProlPermissionErrorException("modify", "static_procedure", newAtom(signature));
    }

    base.assertZ(goal.getContext(), (TermStruct) termToRemove.makeCloneAndVarBound());

    return true;
  }

  @Predicate(signature = "retract/1", template = {"@clause"}, reference = "Retract the first clause which can be unified with argument. True if there is such clause in the knowledge base.")
  @PredicateSynonyms(signatures = "retracta/1")
  @Determined
  public static boolean predicateRETRACT1(final ChoicePoint goal, final TermStruct predicate) {
    final KnowledgeBase base = goal.getContext().getKnowledgeBase();

    Term atom = predicate.getElement(0).findNonVarOrSame();

    if (atom.getTermType() != STRUCT) {
      atom = newStruct(atom);
    }

    final String signature = ((TermStruct) atom).isClause() ? ((TermStruct) atom).getElement(0).getSignature() : atom.getSignature();

    // check that we doesn't overload any static system predicate
    if (goal.getContext().hasPredicateAtLibraryForSignature(signature)) {
      throw new ProlPermissionErrorException("modify", "static_procedure", newAtom(signature));
    }

    return base.retractA(goal.getContext(), (TermStruct) atom.makeCloneAndVarBound());
  }

  @Predicate(signature = "retractz/1", template = {"@clause"}, reference = "Retract the last clause which can be unified with argument. True if there is such clause in the knowledge base.")
  @Determined
  public static boolean predicateRETRACTZ(final ChoicePoint goal, final TermStruct predicate) {
    final KnowledgeBase base = goal.getContext().getKnowledgeBase();

    Term atom = predicate.getElement(0).findNonVarOrSame();

    if (atom.getTermType() != STRUCT) {
      atom = newStruct(atom);
    }

    final String signature = ((TermStruct) atom).isClause() ? ((TermStruct) atom).getElement(0).getSignature() : atom.getSignature();

    // check that we doesn't overload any static system predicate
    if (goal.getContext().hasPredicateAtLibraryForSignature(signature)) {
      throw new ProlPermissionErrorException("modify", "static_procedure", newAtom(signature));
    }

    return base.retractZ(goal.getContext(), (TermStruct) atom);
  }

  @Predicate(signature = "retractall/1", template = {"@clause"}, reference = "Retract all clauses which can be unified with argument. True if there is as minimum one clause in the knowledge base.")
  @Determined
  public static boolean predicateRETRACTALL(final ChoicePoint goal, final TermStruct predicate) {
    final KnowledgeBase base = goal.getContext().getKnowledgeBase();
    Term atom = predicate.getElement(0).findNonVarOrSame();

    if (atom.getTermType() != STRUCT) {
      atom = newStruct(atom);
    }

    final String signature = ((TermStruct) atom).isClause() ? ((TermStruct) atom).getElement(0).getSignature() : atom.getSignature();

    // check that we doesn't overload any static system predicate
    if (goal.getContext().hasPredicateAtLibraryForSignature(signature)) {
      throw new ProlPermissionErrorException("modify", "static_procedure", newAtom(signature));
    }

    return base.retractAll(goal.getContext(), (TermStruct) atom);
  }

  @Predicate(signature = "catch/3", template = "+callable_term,?term,+callable_term", reference = "A goal catch(Goal, Catcher, Handler) is true if\n1. call(Goal) is true, or\n2. An exception is raised which throws a Ball that is caught by Catcher and Handler then succeeds ")
  public static boolean predicateCATCH(final ChoicePoint goal, final TermStruct predicate) {
    ChoicePoint catchGoal = goal.getPayload();

    final Term catching = predicate.getElement(0).findNonVarOrSame();
    final Term catcher = predicate.getElement(1).findNonVarOrSame();
    final Term solver = predicate.getElement(2).findNonVarOrSame();

    if (catchGoal == null) {
      catchGoal = new ChoicePoint(catching, goal.getContext());
      goal.setPayload(catchGoal);
    }

    if (catchGoal.getGoalTerm() == solver) {
      final Term result = catchGoal.next();

      if (result == null) {
        goal.cutVariants();
        return false;
      } else {
        if (catchGoal.isCompleted()) {
          goal.cutVariants();
        }
        return true;
      }
    } else {

      try {
        final Term result = catchGoal.next();
        if (result == null) {
          goal.cutVariants();
          return false;
        } else {
          if (catchGoal.isCompleted()) {
            goal.cutVariants();
          }
          return true;
        }
      } catch (ProlAbstractCatcheableException ex) {
        // exception was thrown
        if (catcher.unifyTo(ex.getAsStruct())) {
          catchGoal = new ChoicePoint(solver, goal.getContext());
          goal.setPayload(catchGoal);
          final Term result = catchGoal.next();
          if (result == null) {
            goal.cutVariants();
            return false;
          } else {
            if (catchGoal.isCompleted()) {
              goal.cutVariants();
            }
            goal.setPayload(catchGoal);
            return true;
          }
        } else {
          throw ex;
        }
      }
    }
  }

  @Predicate(signature = "throw/1", template = "+callable_term", reference = "Throw an exception which can be catched by catch/3")
  @Determined
  public static void predicateTHROW(final ChoicePoint goal, final TermStruct predicate) {
    final Term term = predicate.getElement(0).findNonVarOrSame();
    final String exceptionSignature = term.getSignature();

    if ("instantiation_error/0".equals(exceptionSignature)) {
      throw new ProlInstantiationErrorException(predicate);
    }

    if ("type_error/2".equals(exceptionSignature)) {
      throw new ProlTypeErrorException(predicate.getElement(0).forWrite(), predicate.getElement(1));
    }

    if ("domain_error/2".equals(exceptionSignature)) {
      throw new ProlDomainErrorException(predicate.getElement(0).forWrite(), predicate.getElement(1));
    }

    if ("permission_error/3".equals(exceptionSignature)) {
      throw new ProlPermissionErrorException(predicate.getElement(0).forWrite(), predicate.getElement(1).forWrite(), predicate.getElement(2));
    }

    if ("representation_error/1".equals(exceptionSignature)) {
      throw new ProlRepresentationErrorException(predicate.getElement(0).forWrite(), predicate);
    }

    if ("evaluation_error/1".equals(exceptionSignature)) {
      throw new ProlEvaluationErrorException(predicate.getElement(0).forWrite(), predicate);
    }

    // all other errors make as custom
    //-------------------------------------
    Term arg = predicate.getElement(0);
    if (arg.getTermType() != STRUCT) {
      arg = newStruct(arg);
    }
    throw new ProlCustomErrorException(arg, predicate);
  }

  @Predicate(signature = "pause/1", template = {"+number"}, reference = "Make pause for defined milliseconds.")
  @Determined
  public static void predicatePAUSE(final ChoicePoint goal, final TermStruct predicate) throws InterruptedException {
    final long milliseconds = predicate.getElement(0).findNonVarOrSame().toNumber().longValue();
    if (milliseconds > 0) {
      Thread.sleep(milliseconds);
    }
  }

  // internal auxiliary function for facts/1 and rules/1 predicates
  private static TermStruct processIterator(final ChoicePoint goal, final Iterator<TermStruct> iterator) {
    TermStruct result = null;
    if (iterator.hasNext()) {
      result = iterator.next();
    } else {
      goal.cutVariants();
    }
    return result;
  }

  @Predicate(signature = "facts/1", template = {"+callable_term"}, reference = "Finds only facts at the knowledge base.")
  public static boolean predicateFACTS(final ChoicePoint goal, final TermStruct predicate) {
    final Term callableTerm = predicate.getElement(0).findNonVarOrSame();

    Iterator<TermStruct> factIterator = goal.getPayload();
    if (factIterator == null) {
      Term term = callableTerm;
      factIterator = goal.getContext()
          .getKnowledgeBase()
          .iterate(goal.getContext().getKnowledgeContext(), IteratorType.FACTS, (TermStruct) term);

      if (factIterator == null) {
        goal.cutVariants();
        return false;
      } else {
        goal.setPayload(factIterator);
      }
    }

    boolean result = false;
    final TermStruct nextFact = processIterator(goal, factIterator);
    if (nextFact == null) {
      goal.cutVariants();
    } else {
      result = assertUnify(callableTerm, nextFact);
    }

    return result;
  }

  @Predicate(signature = "regtrigger/3", template = {"+predicate_indicator,+triggerevent,+callable_term"}, reference = "regtrigger(somepredicate/3,onassert,triggerhandler) is always true. The predicate allows to register a trigger handler for distinguished predicate signature. The handled trigger event can be selected from the list [onassert, onretract, onassertretract].")
  @Determined
  public static boolean predicateREGTRIGGER3(final ChoicePoint goal, final TermStruct predicate) {
    final String signature = Utils.extractPredicateSignatureFromStructure(predicate.getElement(0));
    final String triggeringEvent = predicate.getElement(1).findNonVarOrSame().getText();
    final Term callableTerm = predicate.getElement(2).findNonVarOrSame();
    final JProlContext context = goal.getContext();

    final JProlTriggeringEventObserver deferredTriggeringGoal = new JProlTriggeringEventObserver(callableTerm);

    if (triggeringEvent != null) {
      switch (triggeringEvent) {
        case "onassert":
          deferredTriggeringGoal.addSignature(signature, JProlTriggerType.TRIGGER_ASSERT);
          break;
        case "onretract":
          deferredTriggeringGoal.addSignature(signature, JProlTriggerType.TRIGGER_RETRACT);
          break;
        case "onassertretract":
          deferredTriggeringGoal.addSignature(signature, JProlTriggerType.TRIGGER_ASSERT_RETRACT);
          break;
        default:
          throw new ProlCriticalError("Unsupported trigger event detected [" + triggeringEvent + ']');
      }
    }

    context.registerTrigger(deferredTriggeringGoal);

    return true;
  }

  @Predicate(signature = "copy_term/2", template = {"?term,?term"}, reference = "copy_term(X,Y) is true if and only if Y unifies with a term T which is a renamed copy of X.")
  @Determined
  public final boolean predicateCOPYTERM2(final ChoicePoint goal, final TermStruct predicate) {
    final Term in = predicate.getElement(0).findNonVarOrSame().makeClone();
    final Term out = predicate.getElement(1).findNonVarOrSame();
    return in.unifyTo(out);
  }

  @Predicate(signature = "\\+/1", template = "+callable_term", reference = "\\+(Term) is true if and only if call(Term) is false.")
  @Determined
  public final boolean predicateCannotBeProven1(final ChoicePoint goal, final TermStruct predicate) {
    final Term argument = predicate.getElement(0);
    final ChoicePoint subgoal = new ChoicePoint(argument, goal.getContext());
    return subgoal.next() == null;
  }
}
