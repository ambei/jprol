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

package com.igormaznitsa.prol.kbase;

import com.igormaznitsa.prol.data.Term;
import com.igormaznitsa.prol.data.TermOperator;
import com.igormaznitsa.prol.data.TermOperatorContainer;
import com.igormaznitsa.prol.data.TermStruct;
import com.igormaznitsa.prol.logic.ProlContext;
import com.igormaznitsa.prologparser.tokenizer.OpAssoc;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

public interface KnowledgeBase {

  String getId();

  boolean removeOperator(String name, OpAssoc type);

  void addOperator(ProlContext context, TermOperator operator);

  TermOperatorContainer findOperatorForName(ProlContext context, String name);

  boolean hasOperatorStartsWith(ProlContext context, String str);

  void write(PrintWriter writer);

  ClauseIterator getClauseIterator(ClauseIteratorType type, TermStruct template);

  List<TermStruct> findAllForPredicateIndicator(final Term predicateIndicator);

  List<TermStruct> findAllForSignature(final String signature);

  boolean assertZ(ProlContext context, TermStruct clause);

  boolean assertA(ProlContext context, TermStruct clause);

  boolean retractAll(ProlContext context, TermStruct clause);

  boolean retractA(ProlContext context, TermStruct clause);

  boolean retractZ(ProlContext context, TermStruct clause);

  void abolish(ProlContext context, String signature);

  Iterator<TermOperatorContainer> getOperatorIterator();

  KnowledgeBase makeCopy();
}