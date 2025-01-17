/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.hudi;

import com.google.common.collect.ImmutableMap;
import io.trino.plugin.hudi.testing.TpchHudiTablesInitializer;
import io.trino.testing.BaseConnectorTest;
import io.trino.testing.QueryRunner;
import io.trino.testing.TestingConnectorBehavior;
import org.testng.annotations.Test;

import static io.trino.plugin.hudi.HudiQueryRunner.createHudiQueryRunner;
import static io.trino.plugin.hudi.testing.HudiTestUtils.COLUMNS_TO_HIDE;
import static org.apache.hudi.common.model.HoodieTableType.COPY_ON_WRITE;
import static org.assertj.core.api.Assertions.assertThat;

public class TestHudiConnectorTest
        extends BaseConnectorTest
{
    @Override
    protected QueryRunner createQueryRunner()
            throws Exception
    {
        return createHudiQueryRunner(
                ImmutableMap.of(),
                ImmutableMap.of("hudi.columns-to-hide", COLUMNS_TO_HIDE),
                new TpchHudiTablesInitializer(COPY_ON_WRITE, REQUIRED_TPCH_TABLES));
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Override
    protected boolean hasBehavior(TestingConnectorBehavior connectorBehavior)
    {
        switch (connectorBehavior) {
            case SUPPORTS_INSERT:
            case SUPPORTS_DELETE:
            case SUPPORTS_UPDATE:
            case SUPPORTS_MERGE:
                return false;

            case SUPPORTS_TOPN_PUSHDOWN:
            case SUPPORTS_DEREFERENCE_PUSHDOWN:
                return false;

            case SUPPORTS_CREATE_SCHEMA:
                return false;

            case SUPPORTS_CREATE_TABLE:
            case SUPPORTS_RENAME_TABLE:
                return false;

            case SUPPORTS_ADD_COLUMN:
            case SUPPORTS_RENAME_COLUMN:
            case SUPPORTS_SET_COLUMN_TYPE:
                return false;

            case SUPPORTS_CREATE_VIEW:
            case SUPPORTS_CREATE_MATERIALIZED_VIEW:
                return false;

            case SUPPORTS_COMMENT_ON_TABLE:
            case SUPPORTS_COMMENT_ON_COLUMN:
                return false;

            default:
                return super.hasBehavior(connectorBehavior);
        }
    }

    @Test
    @Override
    public void testShowCreateTable()
    {
        String schema = getSession().getSchema().orElseThrow();
        assertThat((String) computeScalar("SHOW CREATE TABLE orders"))
                .matches("\\QCREATE TABLE hudi." + schema + ".orders (\n" +
                        "   orderkey bigint,\n" +
                        "   custkey bigint,\n" +
                        "   orderstatus varchar(1),\n" +
                        "   totalprice double,\n" +
                        "   orderdate date,\n" +
                        "   orderpriority varchar(15),\n" +
                        "   clerk varchar(15),\n" +
                        "   shippriority integer,\n" +
                        "   comment varchar(79)\n" +
                        ")\n" +
                        "WITH (\n" +
                        "   location = \\E'.*/orders'\n\\Q" +
                        ")");
    }

    @Test
    public void testHideHiveSysSchema()
    {
        assertThat(computeActual("SHOW SCHEMAS").getOnlyColumnAsSet()).doesNotContain("sys");
        assertQueryFails("SHOW TABLES IN hudi.sys", ".*Schema 'sys' does not exist");
    }
}
