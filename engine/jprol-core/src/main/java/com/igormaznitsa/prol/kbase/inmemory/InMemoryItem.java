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

package com.igormaznitsa.prol.kbase.inmemory;

import com.igormaznitsa.prol.data.Term;
import com.igormaznitsa.prol.data.TermStruct;
import lombok.Data;

import java.io.PrintWriter;

import static java.util.Objects.requireNonNull;

@Data
public final class InMemoryItem {
  private final TermStruct clause;
  private final Term keyTerm;
  private final boolean rightPartPresented;

  InMemoryItem(final TermStruct clause) {
    super();
    this.clause = clause;

    if (clause.isClause()) {
      this.rightPartPresented = true;
      this.keyTerm = clause.getElement(0);
    } else {
      this.rightPartPresented = false;
      this.keyTerm = clause;
    }
  }

  InMemoryItem makeClone() {
    return new InMemoryItem((TermStruct) this.clause.makeClone());
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(final Object that) {
    return this == that;
  }

  @Override
  public String toString() {
    return this.keyTerm.toString();
  }

  void write(final PrintWriter writer) {
    requireNonNull(writer, "Writer must not be null")
        .write(String.format("%s.%n", this.clause.toSrcString()));
  }
}