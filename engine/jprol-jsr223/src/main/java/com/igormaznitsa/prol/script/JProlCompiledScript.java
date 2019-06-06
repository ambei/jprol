package com.igormaznitsa.prol.script;

import com.igormaznitsa.prol.data.Term;
import com.igormaznitsa.prol.logic.Goal;
import com.igormaznitsa.prol.logic.ProlContext;
import com.igormaznitsa.prol.parser.ProlReader;
import com.igormaznitsa.prol.parser.ProlTreeBuilder;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public final class JProlCompiledScript extends CompiledScript {

  private final JProlScriptEngine engine;
  private final Term compiled;

  JProlCompiledScript(final String script, final JProlScriptEngine engine) throws ScriptException {
    super();
    this.engine = engine;
    this.compiled = parseScript(new ProlReader(script), ((JProlScriptContext) engine.getContext()).getProlContext());
  }

  JProlCompiledScript(final Reader script, final JProlScriptEngine engine) throws ScriptException {
    super();
    this.engine = engine;
    this.compiled = parseScript(new ProlReader(script), ((JProlScriptContext) engine.getContext()).getProlContext());
  }

  private static Term parseScript(final ProlReader reader, final ProlContext context) throws ScriptException {
    try {
      return new ProlTreeBuilder(context).readPhraseAndMakeTree(reader);
    } catch (IOException ex) {
      throw new ScriptException(ex);
    }
  }

  @Override
  public Object eval(final ScriptContext context) throws ScriptException {
    final JProlScriptContext jprolContext = (JProlScriptContext) context;
    try {
      final Map<String, Term> predefinedValues = new HashMap<>();
      JProlScriptEngine.fillGoalByBindings(jprolContext.getJprolBindings(ScriptContext.GLOBAL_SCOPE), predefinedValues);
      JProlScriptEngine.fillGoalByBindings(jprolContext.getJprolBindings(ScriptContext.ENGINE_SCOPE), predefinedValues);
      final Goal preparedGoal = new Goal(this.compiled.makeClone(), jprolContext.getProlContext(), predefinedValues, null);
      
      final Object result = preparedGoal.solve();
      
      if (result != null) {
        jprolContext.getJprolBindings(ScriptContext.ENGINE_SCOPE).fillByValues(preparedGoal.findAllInstantiatedVars());
      }
      
      return result;
    } catch (InterruptedException ex) {
      return null;
    }
  }

  @Override
  public ScriptEngine getEngine() {
    return this.engine;
  }

}