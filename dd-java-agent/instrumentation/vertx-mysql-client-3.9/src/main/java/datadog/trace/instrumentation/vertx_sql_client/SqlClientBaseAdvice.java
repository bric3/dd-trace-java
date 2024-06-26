package datadog.trace.instrumentation.vertx_sql_client;

import datadog.trace.api.Pair;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import datadog.trace.bootstrap.instrumentation.jdbc.DBQueryInfo;
import io.vertx.mysqlclient.MySQLConnection;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.SqlClient;
import net.bytebuddy.asm.Advice;

public class SqlClientBaseAdvice {
  public static class NormalQuery {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void afterQuery(
        @Advice.This final SqlClient zis,
        @Advice.Argument(0) final String sql,
        @Advice.Return final Query query) {
      Pair<DBInfo, DBQueryInfo> info =
          Pair.of(
              InstrumentationContext.get(SqlClient.class, DBInfo.class).get(zis),
              DBQueryInfo.ofStatement(sql));
      InstrumentationContext.get(Query.class, Pair.class).put(query, info);
    }

    // Limit ourselves to 3.9.x and MySQL by checking for this method that was removed in 4.x
    private static void muzzleCheck(MySQLConnection connection) {
      connection.close();
    }
  }

  public static class PreparedQuery {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void afterPreparedQuery(
        @Advice.This final SqlClient zis,
        @Advice.Argument(0) final String sql,
        @Advice.Return final Query query) {
      Pair<DBInfo, DBQueryInfo> info =
          Pair.of(
              InstrumentationContext.get(SqlClient.class, DBInfo.class).get(zis),
              DBQueryInfo.ofPreparedStatement(sql));
      InstrumentationContext.get(Query.class, Pair.class).put(query, info);
    }

    // Limit ourselves to 3.9.x and MySQL by checking for this method that was removed in 4.x
    private static void muzzleCheck(MySQLConnection connection) {
      connection.close();
    }
  }
}
