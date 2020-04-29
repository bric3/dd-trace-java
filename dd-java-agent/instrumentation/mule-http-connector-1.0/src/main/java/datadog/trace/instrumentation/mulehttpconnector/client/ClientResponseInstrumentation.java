package datadog.trace.instrumentation.mulehttpconnector.client;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperClass;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.ParentChildSpan;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public final class ClientResponseInstrumentation extends Instrumenter.Default {

  public ClientResponseInstrumentation() {
    super("mule-http-connector");
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap(
        "com.ning.http.client.AsyncCompletionHandler", ParentChildSpan.class.getName());
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return hasSuperClass(named("com.ning.http.client.AsyncCompletionHandler"));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {packageName + ".ClientDecorator"};
  }

  // TO-DO: might need to specify that it is a nested method
  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("onCompleted")
            .and(takesArgument(0, named("com.ning.http.client.Response")))
            .and(isPublic()),
        packageName + ".ClientResponseAdvice");
  }
}
