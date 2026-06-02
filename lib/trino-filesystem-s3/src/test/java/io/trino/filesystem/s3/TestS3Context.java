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
package io.trino.filesystem.s3;

import io.trino.spi.security.ConnectorIdentity;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;

import java.util.Optional;

import static io.trino.filesystem.s3.S3FileSystemConfig.ObjectCannedAcl.NONE;
import static io.trino.filesystem.s3.S3FileSystemConfig.StorageClassType.STANDARD;
import static io.trino.filesystem.s3.S3FileSystemConstants.EXTRA_CREDENTIALS_ACCESS_KEY_PROPERTY;
import static io.trino.filesystem.s3.S3FileSystemConstants.EXTRA_CREDENTIALS_REFRESH_KEY_PROPERTY;
import static io.trino.filesystem.s3.S3FileSystemConstants.EXTRA_CREDENTIALS_SECRET_KEY_PROPERTY;
import static io.trino.filesystem.s3.S3FileSystemConstants.EXTRA_CREDENTIALS_SESSION_TOKEN_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;

final class TestS3Context
{
    @Test
    void testRegisteredRefreshProviderOverridesStaticCredentials()
    {
        S3CredentialsProviderRegistry.getInstance().clear();
        AwsCredentialsProvider refreshableProvider = () -> AwsSessionCredentials.create("refresh-access", "refresh-secret", "refresh-token");
        S3CredentialsProviderRegistry.getInstance().register("refresh-key", refreshableProvider);
        ConnectorIdentity identity = ConnectorIdentity.forUser("test")
                .withExtraCredentials(java.util.Map.of(
                        EXTRA_CREDENTIALS_ACCESS_KEY_PROPERTY, "static-access",
                        EXTRA_CREDENTIALS_SECRET_KEY_PROPERTY, "static-secret",
                        EXTRA_CREDENTIALS_SESSION_TOKEN_PROPERTY, "static-token",
                        EXTRA_CREDENTIALS_REFRESH_KEY_PROPERTY, "refresh-key"))
                .build();

        S3Context context = new S3Context(
                5 * 1024 * 1024,
                false,
                S3Context.S3SseContext.of(S3FileSystemConfig.S3SseType.NONE, null, null),
                Optional.empty(),
                STANDARD,
                NONE);

        AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
        context.withCredentials(identity).applyCredentialProviderOverride(builder);

        AwsCredentialsProvider resolvedProvider = builder.build().credentialsProvider().orElseThrow();
        assertThat(resolvedProvider.resolveCredentials().accessKeyId()).isEqualTo("refresh-access");
        assertThat(((AwsSessionCredentials) resolvedProvider.resolveCredentials()).sessionToken()).isEqualTo("refresh-token");
        S3CredentialsProviderRegistry.getInstance().clear();
    }

    @Test
    void testMissingRefreshProviderFallsBackToStaticCredentials()
    {
        S3CredentialsProviderRegistry.getInstance().clear();
        ConnectorIdentity identity = ConnectorIdentity.forUser("test")
                .withExtraCredentials(java.util.Map.of(
                        EXTRA_CREDENTIALS_ACCESS_KEY_PROPERTY, "static-access",
                        EXTRA_CREDENTIALS_SECRET_KEY_PROPERTY, "static-secret",
                        EXTRA_CREDENTIALS_SESSION_TOKEN_PROPERTY, "static-token",
                        EXTRA_CREDENTIALS_REFRESH_KEY_PROPERTY, "missing-refresh-key"))
                .build();

        S3Context context = new S3Context(
                5 * 1024 * 1024,
                false,
                S3Context.S3SseContext.of(S3FileSystemConfig.S3SseType.NONE, null, null),
                Optional.empty(),
                STANDARD,
                NONE);

        AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
        context.withCredentials(identity).applyCredentialProviderOverride(builder);

        AwsCredentialsProvider resolvedProvider = builder.build().credentialsProvider().orElseThrow();
        assertThat(resolvedProvider.resolveCredentials().accessKeyId()).isEqualTo("static-access");
        assertThat(((AwsSessionCredentials) resolvedProvider.resolveCredentials()).sessionToken()).isEqualTo("static-token");
        S3CredentialsProviderRegistry.getInstance().clear();
    }
}
