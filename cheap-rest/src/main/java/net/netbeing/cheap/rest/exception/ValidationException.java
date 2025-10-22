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

package net.netbeing.cheap.rest.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when request validation fails.
 * Maps to HTTP 400 Bad Request.
 */
public class ValidationException extends RuntimeException
{
    private final List<ValidationError> errors;

    public ValidationException(String message)
    {
        super(message);
        this.errors = new ArrayList<>();
    }

    public ValidationException(String message, List<ValidationError> errors)
    {
        super(message);
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public ValidationException(List<ValidationError> errors)
    {
        super("Validation failed");
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    public List<ValidationError> getErrors()
    {
        return new ArrayList<>(errors);
    }

    public record ValidationError(
        String field,
        String message
    )
    {
    }
}
