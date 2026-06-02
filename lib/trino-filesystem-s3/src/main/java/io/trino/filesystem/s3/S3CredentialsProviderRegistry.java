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

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

public final class S3CredentialsProviderRegistry
{
    private static final S3CredentialsProviderRegistry INSTANCE = new S3CredentialsProviderRegistry();

    private final ConcurrentMap<String, AwsCredentialsProvider> providers = new ConcurrentHashMap<>();

    private S3CredentialsProviderRegistry() {}

    public static S3CredentialsProviderRegistry getInstance()
    {
        return INSTANCE;
    }

    public void register(String refreshKey, AwsCredentialsProvider credentialsProvider)
    {
        providers.putIfAbsent(requireNonNull(refreshKey, "refreshKey is null"), requireNonNull(credentialsProvider, "credentialsProvider is null"));
    }

    public Optional<AwsCredentialsProvider> get(String refreshKey)
    {
        return Optional.ofNullable(providers.get(requireNonNull(refreshKey, "refreshKey is null")));
    }

    public void clear()
    {
        providers.clear();
    }
}
