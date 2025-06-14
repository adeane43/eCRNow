package java.bsa.kar.condition;

import java.bsa.kar.action.SubmitReport;
import java.bsa.kar.model.BsaAction;
import java.bsa.model.KarProcessingData;
import java.ecrapp.config.SpringConfiguration;
import org.hl7.fhir.r4.model.Expression;
import org.junit.Test;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = SpringConfiguration.class)
public class FhirPathProcessorTest {

  // Autowired to pass to CqlProcessors.
  @Autowired ExpressionEvaluator expressionEvaluator;

  private FhirPathProcessor processor = new FhirPathProcessor();

  @Test
  public void testEvaluateExpression() throws Exception {
    BsaCqlCondition bsaCondition = new BsaCqlCondition();
    bsaCondition.setConditionProcessor(processor);
    bsaCondition.setLogicExpression(new Expression());
    BsaAction action = new SubmitReport(); // TODO: mock action
    KarProcessingData kd = new KarProcessingData();
    // processor.evaluateExpression(bsaCondition, action, kd);
  }
}
