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

package com.igormaznitsa.prol.data;

import static com.igormaznitsa.prol.data.Terms.newLong;

public final class TermLong extends NumericTerm {

  private final long value;

  TermLong(final String name) {
    super(name);
    value = Long.parseLong(name);
  }

  TermLong(final long value) {
    super("");
    this.value = value;
  }

  @Override
  public String toString() {
    final String val = getText();
    if (val.isEmpty()) {
      return Long.toString(value);
    } else {
      return val;
    }
  }

  @Override
  public int hashCode() {
    return (int) this.value ^ (int) (this.value >> 32);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj.getClass() == TermLong.class) {
      return ((TermLong) obj).value == this.value;
    }

    return super.equals(obj);
  }

  @Override
  public boolean stronglyEqualsTo(final Term term) {
    return this == term || (term.getClass() == TermLong.class && this.value == ((TermLong) term).value);
  }

  @Override
  public Number toNumber() {
    return this.value;
  }

  @Override
  public String toSrcString() {
    return Long.toString(value);
  }

  @Override
  public String getText() {
    return Long.toString(this.value);
  }

  @Override
  public int compare(final NumericTerm atom) {
    if (atom.isDouble()) {
      final double value = atom.toNumber().doubleValue();
      return Double.compare((double) this.value, value);
    }
    return Long.compare(this.value, atom.toNumber().longValue());
  }

  @Override
  public NumericTerm add(final NumericTerm atom) {
    if (atom.isDouble()) {
      final double value = atom.toNumber().doubleValue();
      return Terms.newDouble((double) this.value + value);
    } else {
      return newLong(this.value + atom.toNumber().longValue());
    }
  }

  @Override
  public NumericTerm sub(final NumericTerm atom) {
    if (atom.isDouble()) {
      final double value = atom.toNumber().doubleValue();
      return Terms.newDouble((double) this.value - value);
    } else {
      return newLong(this.value - atom.toNumber().longValue());
    }
  }

  @Override
  public NumericTerm div(final NumericTerm atom) {
    if (atom.isDouble()) {
      final double value = atom.toNumber().doubleValue();
      return Terms.newDouble((double) this.value / value);
    } else {
      return newLong(this.value / atom.toNumber().longValue());
    }
  }

  @Override
  public NumericTerm mul(final NumericTerm atom) {
    if (atom.isDouble()) {
      final double value = atom.toNumber().doubleValue();
      return Terms.newDouble((double) this.value * value);
    } else {
      return newLong(this.value * atom.toNumber().longValue());
    }
  }

  @Override
  public NumericTerm neg() {
    return newLong(-this.value);
  }

  @Override
  public boolean isDouble() {
    return false;
  }

  @Override
  public int compareTermTo(Term atom) {
    if (this == atom) {
      return 0;
    }

    atom = atom.findNonVarOrDefault(atom);

    switch (atom.getTermType()) {
      case VAR:
        return 1;
      case ATOM: {
        if (atom instanceof NumericTerm) {
          final long value = ((NumericTerm) atom).isDouble() ? Math.round(atom.toNumber().doubleValue()) :
              atom.toNumber().longValue();
          return Long.compare(this.value, value);
        } else {
          return -1;
        }
      }
      default:
        return -1;
    }
  }

  @Override
  public NumericTerm abs() {
    if (this.value >= 0L) {
      return this;
    }
    return newLong(Math.abs(this.value));
  }

  @Override
  public NumericTerm sign() {
    int sign = 0;
    if (this.value < 0L) {
      sign = -1;
    } else if (this.value > 0L) {
      sign = 1;
    }
    return newLong(sign);
  }
}
