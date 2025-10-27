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

package net.netbeing.cheap.rest.client.exception;

/**
 * Exception thrown when a request is invalid (HTTP 400).
 */
@SuppressWarnings("unused")
public class CheapRestBadRequestException extends CheapRestClientException
{
    /**
     * Creates a new exception with the specified message.
     *
     * @param message the error message
     */
    public CheapRestBadRequestException(String message)
    {
        super(message);
    }

    /**
     * Creates a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public CheapRestBadRequestException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
