/*
 * Copyright 2018 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.openid.appauth.AuthorizationException.GeneralErrors;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * An OpenID Connect ID Token. Contains claims about the authentication of an End-User by an
 * Authorization Server. Supports parsing ID Tokens from JWT Compact Serializations and validation
 * according to the OpenID Connect specification.
 *
 * @see "OpenID Connect Core ID Token, Section 2
 * <http://openid.net/specs/openid-connect-core-1_0.html#IDToken>"
 * @see "OpenID Connect Core ID Token Validation, Section 3.1.3.7
 * <http://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation>"
 */
class IdToken {

    private static final String KEY_ISSUER = "iss";
    private static final String KEY_SUBJECT = "sub";
    private static final String KEY_AUDIENCE = "aud";
    private static final String KEY_EXPIRATION = "exp";
    private static final String KEY_ISSUED_AT = "iat";
    private static final String KEY_NONCE = "nonce";
    private static final Long MILLIS_PER_SECOND = 1000L;
    private static final Long TEN_MINUTES_IN_SECONDS = 600L;

    public final String issuer;
    public final String subject;
    public final List<String> audience;
    public final Long expiration;
    public final Long issuedAt;
    public final String nonce;

    IdToken(@NonNull String issuer,
            @NonNull String subject,
            @NonNull List<String> audience,
            @NonNull Long expiration,
            @NonNull Long issuedAt,
            @Nullable String nonce) {
        this.issuer = issuer;
        this.subject = subject;
        this.audience = audience;
        this.expiration = expiration;
        this.issuedAt = issuedAt;
        this.nonce = nonce;
    }

    private static JSONObject parseJwtSection(String section) throws JSONException {
        byte[] decodedSection = Base64.decode(section,Base64.URL_SAFE);
        String jsonString = new String(decodedSection);
        return new JSONObject(jsonString);
    }

    static IdToken from(String token) throws JSONException, IdTokenException {
        String[] sections = token.split("\\.");

        if (sections.length <= 1) {
            throw new IdTokenException("ID token must have both header and claims section");
        }

        // We ignore header contents, but parse it to check that it is structurally valid JSON
        parseJwtSection(sections[0]);
        JSONObject claims = parseJwtSection(sections[1]);

        String issuer = JsonUtil.getString(claims, KEY_ISSUER);
        String subject = JsonUtil.getString(claims, KEY_SUBJECT);
        List<String> audience;
        try {
            audience = JsonUtil.getStringList(claims, KEY_AUDIENCE);
        } catch (JSONException jsonEx) {
            audience = new ArrayList<>();
            audience.add(JsonUtil.getString(claims, KEY_AUDIENCE));
        }
        Long expiration = claims.getLong(KEY_EXPIRATION);
        Long issuedAt = claims.getLong(KEY_ISSUED_AT);
        String nonce = JsonUtil.getStringIfDefined(claims, KEY_NONCE);

        return new IdToken(
            issuer,
            subject,
            audience,
            expiration,
            issuedAt,
            nonce
        );
    }

    void validate(@NonNull TokenRequest tokenRequest, Clock clock) throws AuthorizationException {
        // no-op
    }

    static class IdTokenException extends Exception {
        IdTokenException(String message) {
            super(message);
        }
    }
}
