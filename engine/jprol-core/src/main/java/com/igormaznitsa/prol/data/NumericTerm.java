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

import static com.igormaznitsa.prol.data.TermType.VAR;

public abstract class NumericTerm extends Term {

  public NumericTerm(final String text) {
    super(text);
  }

  public abstract int compare(NumericTerm term);

  public abstract NumericTerm add(NumericTerm term);

  public abstract NumericTerm sub(NumericTerm term);

  public abstract NumericTerm div(NumericTerm term);

  public abstract NumericTerm mul(NumericTerm term);

  public abstract NumericTerm neg();

  public abstract NumericTerm abs();

  public abstract NumericTerm sign();

  public abstract boolean isDouble();

  @SuppressWarnings("unchecked")
  @Override
  public final <T> T toObject() {
    return (T) this.toNumber();
  }

  @Override
  public final String forWrite() {
    return this.getText();
  }

  @Override
  public final boolean dryUnifyTo(Term atom) {
    if (this == atom) {
      return true;
    }

    if (atom.getTermType() == VAR) {
      atom = ((Var) atom).getValue();
    }

    if (atom == null) {
      return true;
    }

    if (atom.getClass() == this.getClass()) {
      return this.compare((NumericTerm) atom) == 0;
    }
    return false;
  }

  @Override
  public final boolean unifyTo(Term atom) {
    if (this == atom) {
      return true;
    }

    switch (atom.getTermType()) {
      case ATOM: {
        if (atom.getClass() == this.getClass()) {
          return this.compare((NumericTerm) atom) == 0;
        } else {
          return false;
        }
      }
      case VAR: {
        final Var var = (Var) atom;
        final Term value = var.getValue();
        if (value == null) {
          return ((Var) atom).setValue(this);
        } else {
          return unifyTo(value);
        }
      }
    }
    return false;
  }

}
