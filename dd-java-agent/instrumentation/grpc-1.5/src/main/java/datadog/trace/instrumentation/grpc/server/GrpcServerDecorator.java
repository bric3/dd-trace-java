package datadog.trace.instrumentation.grpc.server;

import datadog.trace.api.Config;
import datadog.trace.api.Function;
import datadog.trace.api.cache.DDCache;
import datadog.trace.api.cache.DDCaches;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.ServerDecorator;
import io.grpc.ServerCall;
import io.grpc.Status;

public class GrpcServerDecorator extends ServerDecorator {

  private static final boolean TRIM_RESOURCE_PACKAGE_NAME =
      Config.get().isGrpcServerTrimPackageResource();

  public static final CharSequence GRPC_SERVER = UTF8BytesString.create("grpc.server");
  public static final CharSequence COMPONENT_NAME = UTF8BytesString.create("grpc-server");
  public static final CharSequence GRPC_MESSAGE = UTF8BytesString.create("grpc.message");
  public static final GrpcServerDecorator DECORATE = new GrpcServerDecorator();

  private static final Function<String, String> NORMALIZE =
      new Function<String, String>() {
        @Override
        public String apply(String fullName) {
          int index = fullName.lastIndexOf(".");
          if (index > 0) {
            return fullName.substring(index + 1);
          } else {
            return fullName;
          }
        }
      };

  private final DDCache<String, String> cachedResourceNames;

  public GrpcServerDecorator() {
    if (TRIM_RESOURCE_PACKAGE_NAME) {
      cachedResourceNames = DDCaches.newFixedSizeCache(512);
    } else {
      cachedResourceNames = null;
    }
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"grpc", "grpc-server"};
  }

  @Override
  protected CharSequence spanType() {
    return InternalSpanTypes.RPC;
  }

  @Override
  protected CharSequence component() {
    return COMPONENT_NAME;
  }

  @Override
  public AgentSpan afterStart(final AgentSpan span) {
    span.setMeasured(true);
    return super.afterStart(span);
  }

  public <RespT, ReqT> AgentSpan onCall(final AgentSpan span, ServerCall<ReqT, RespT> call) {
    if (TRIM_RESOURCE_PACKAGE_NAME) {
      span.setResourceName(
          cachedResourceNames.computeIfAbsent(
              call.getMethodDescriptor().getFullMethodName(), NORMALIZE));
    } else {
      span.setResourceName(call.getMethodDescriptor().getFullMethodName());
    }
    return span;
  }

  public AgentSpan onClose(final AgentSpan span, final Status status) {

    span.setTag("status.code", status.getCode().name());
    span.setTag("status.description", status.getDescription());

    if (!status.isOk() && (Status.Code.CANCELLED != status.getCode())) {
      onError(span, status.getCause());
      span.setError(true);
    }

    return span;
  }
}
