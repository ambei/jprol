/*
 * Copyright 2019 Igor Maznitsa.
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
package com.igormaznitsa.prol.script;

import com.igormaznitsa.prol.data.Term;
import com.igormaznitsa.prol.data.Var;
import com.igormaznitsa.prol.logic.Goal;
import com.igormaznitsa.prol.parser.ProlReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

final class JProlScriptEngine extends AbstractScriptEngine implements Compilable {

  private final JProlScriptEngineFactory factory;

  public JProlScriptEngine(final JProlScriptEngineFactory factory) {
    super();
    this.factory = factory;
    this.context = new JProlScriptContext("prol-script-context");
  }

  private Object solveGoal(final ProlReader reader, final JProlScriptContext context) throws ScriptException {
    try {
      final Map<String, Term> varValues = new HashMap<>();

      fillGoalByBindings(context.getJprolBindings(ScriptContext.GLOBAL_SCOPE), varValues);
      fillGoalByBindings(context.getJprolBindings(ScriptContext.ENGINE_SCOPE), varValues);

      final Goal goal = new Goal(reader, context.getProlContext(), varValues.isEmpty() ? null : varValues);
      final Object result = goal.solve();

      if (result!=null){
        context.getJprolBindings(ScriptContext.ENGINE_SCOPE).fillByValues(goal.findAllInstantiatedVars());
      }

      return result;
    } catch (IOException ex) {
      throw new ScriptException(ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  
  static void fillGoalByBindings(final JProlBindings bindings, final Map<String, Term> map) {
    if (bindings != null) {
      map.putAll(bindings.getTermMap());
    }
  }

  @Override
  public Object eval(final String script, final ScriptContext context) throws ScriptException {
    return solveGoal(new ProlReader(script), (JProlScriptContext) context);
  }

  @Override
  public Object eval(final Reader reader, final ScriptContext context) throws ScriptException {
    return solveGoal(new ProlReader(reader), (JProlScriptContext) context);
  }

  @Override
  public Bindings createBindings() {
    return new SimpleBindings();
  }

  @Override
  public ScriptEngineFactory getFactory() {
    return this.factory;
  }

  @Override
  public CompiledScript compile(final String script) throws ScriptException {
    return new JProlCompiledScript(script, this);
  }

  @Override
  public CompiledScript compile(final Reader script) throws ScriptException {
    return new JProlCompiledScript(script, this);
  }

}
