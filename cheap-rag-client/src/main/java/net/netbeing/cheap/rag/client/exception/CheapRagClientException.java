/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.rag.client.exception;

/**
 * Base exception for all CHEAP RAG client errors.
 * This is a runtime exception to avoid forcing clients to handle checked exceptions
 * for what are typically unrecoverable HTTP communication errors.
 */
public class CheapRagClientException extends RuntimeException
{
    public CheapRagClientException(String message)
    {
        super(message);
    }

    public CheapRagClientException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public CheapRagClientException(Throwable cause)
    {
        super(cause);
    }
}
