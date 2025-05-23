/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.mc.java.protoc;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.base.RejectionMessage;
import io.spine.base.SubscribableField;
import io.spine.base.UuidValue;
import io.spine.query.EntityColumn;
import io.spine.test.tools.mc.java.protoc.CreateUser;
import io.spine.test.tools.mc.java.protoc.CustomerName;
import io.spine.test.tools.mc.java.protoc.CustomerNameOrBuilder;
import io.spine.test.tools.mc.java.protoc.EducationalInstitution;
import io.spine.test.tools.mc.java.protoc.Kindergarten;
import io.spine.test.tools.mc.java.protoc.MFGTMessage;
import io.spine.test.tools.mc.java.protoc.MessageEnhancedWithPrefixGenerations;
import io.spine.test.tools.mc.java.protoc.MessageEnhancedWithRegexGenerations;
import io.spine.test.tools.mc.java.protoc.MessageEnhancedWithSuffixGenerations;
import io.spine.test.tools.mc.java.protoc.Movie;
import io.spine.test.tools.mc.java.protoc.MovieTitleChanged;
import io.spine.test.tools.mc.java.protoc.NotifyUser;
import io.spine.test.tools.mc.java.protoc.Outer;
import io.spine.test.tools.mc.java.protoc.UserRejection;
import io.spine.test.tools.mc.java.protoc.DocumentMessage;
import io.spine.test.tools.mc.java.protoc.PrefixedMessage;
import io.spine.test.tools.mc.java.protoc.SuffixedMessage;
import io.spine.test.tools.mc.java.protoc.RegexedMessage;
import io.spine.test.tools.mc.java.protoc.PICreateCustomer;
import io.spine.test.tools.mc.java.protoc.PICreateUser;
import io.spine.test.tools.mc.java.protoc.PICustomerCommand;
import io.spine.test.tools.mc.java.protoc.PICustomerCreated;
import io.spine.test.tools.mc.java.protoc.PICustomerEmailReceived;
import io.spine.test.tools.mc.java.protoc.PICustomerEvent;
import io.spine.test.tools.mc.java.protoc.PICustomerNotified;
import io.spine.test.tools.mc.java.protoc.PIUserCreated;
import io.spine.test.tools.mc.java.protoc.PIUserEvent;
import io.spine.test.tools.mc.java.protoc.PIUserNameUpdated;
import io.spine.test.tools.mc.java.protoc.Rejections;
import io.spine.test.tools.mc.java.protoc.School;
import io.spine.test.tools.mc.java.protoc.TypicalIdentifier;
import io.spine.test.tools.mc.java.protoc.University;
import io.spine.test.tools.mc.java.protoc.UserCreated;
import io.spine.test.tools.mc.java.protoc.UserInfo;
import io.spine.test.tools.mc.java.protoc.UserName;
import io.spine.test.tools.mc.java.protoc.WithUserId;
import io.spine.test.tools.mc.java.protoc.UserNotified;
import io.spine.test.tools.mc.java.protoc.WeatherForecast;
import io.spine.test.tools.mc.java.protoc.Wrapped;
import io.spine.type.MessageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("`ProtocPlugin` should")
final class ProtocPluginTest {

    private static final String EVENT_INTERFACE_FQN =
            "io.spine.test.tools.mc.java.protoc.PICustomerEvent";
    private static final String COMMAND_INTERFACE_FQN =
            "io.spine.test.tools.mc.java.protoc.PICustomerCommand";
    private static final String USER_COMMAND_FQN =
            "io.spine.test.tools.mc.java.protoc.PIUserCommand";

    @Test
    @DisplayName("generate marker interfaces")
    void generateMarkerInterfaces() throws ClassNotFoundException {
        checkMarkerInterface(EVENT_INTERFACE_FQN);
    }

    @Test
    @DisplayName("implement marker interface in the generated messages")
    void implementMarkerInterfacesInGeneratedMessages() {
        assertThat(PICustomerNotified.getDefaultInstance())
             .isInstanceOf(PICustomerEvent.class);
        assertThat(PICustomerEmailReceived.getDefaultInstance())
             .isInstanceOf(PICustomerEvent.class);
    }

    @Test
    @DisplayName("implement interface in the generated messages with `IS` option")
    void implementInterfaceInGeneratedMessagesWithIsOption() {
        var event = PICustomerCreated.getDefaultInstance();
        assertThat(event).isInstanceOf(PICustomerEvent.class);

        var cmd = PICreateCustomer.getDefaultInstance();
        assertThat(cmd).isInstanceOf(PICustomerCommand.class);
    }

    @Test
    @DisplayName("use `IS` and `EVERY IS` together")
    void isAndEveryIsTogether() {
        var event1 = PIUserCreated.getDefaultInstance();
        assertThat(event1).isInstanceOf(PIUserEvent.class);
        assertThat(event1).isInstanceOf(WithUserId.class);

        var event2 = PIUserNameUpdated.getDefaultInstance();
        assertThat(event2)
                .isInstanceOf(PIUserEvent.class);
        assertThat(event2)
                .isInstanceOf(WithUserId.class);
    }

    @Test
    @DisplayName("resolve packages from src proto if the packages are not specified")
    void resolvingPackages() throws ClassNotFoundException {
        var cls = checkMarkerInterface(USER_COMMAND_FQN);
        assertThat(PICreateUser.class)
                .isAssignableTo(cls);
    }

    @Test
    @DisplayName("skip non specified message types")
    void skipNonSpecifiedMessageTypes() {
        Class<?> cls = CustomerName.class;
        List<Class<?>> interfaces = ImmutableList.copyOf(cls.getInterfaces());
        assertThat(interfaces)
                .contains(CustomerNameOrBuilder.class);
    }

    @Nested
    @DisplayName("mark")
    class MarkingWithInterfaces {

        @Test
        @DisplayName("command messages")
        void markCommandMessages() {
            assertThat(CreateUser.getDefaultInstance())
                 .isInstanceOf(CommandMessage.class);
            assertThat(NotifyUser.getDefaultInstance())
                 .isInstanceOf(CommandMessage.class);
        }

        @Test
        @DisplayName("event messages")
        void markMessages() {
            assertThat(UserCreated.getDefaultInstance())
                 .isInstanceOf(EventMessage.class);
            assertThat(UserNotified.getDefaultInstance())
                 .isInstanceOf(EventMessage.class);
        }

        @Test
        @DisplayName("rejection messages")
        void markRejectionMessages() {
            assertThat(Rejections.UserAlreadyExists.getDefaultInstance())
                 .isInstanceOf(RejectionMessage.class);
            assertThat(Rejections.UserAlreadyExists.getDefaultInstance())
                    .isInstanceOf(UserRejection.class);
        }

        @Test
        @DisplayName("mark UUID-based identifiers")
        void markUuids() {
            assertThat(TypicalIdentifier.getDefaultInstance())
                 .isInstanceOf(UuidValue.class);
        }

        @Test
        @DisplayName("messages with already existing interface types")
        @SuppressWarnings("UnnecessaryLocalVariable")
            // Compile-time verification.
        void implementHandcraftedInterfaces() {
            assertThat(Rejections.UserAlreadyExists.getDefaultInstance())
                    .isInstanceOf(UserRejection.class);
            assertFalse(Message.class.isAssignableFrom(UserRejection.class));

            var id = Identifier.newUuid();
            var message = Rejections.UserAlreadyExists.newBuilder()
                    .setId(id)
                    .build();
            UserRejection rejection = message;
            assertEquals(id, rejection.getId());
        }

        @Nested
        @DisplayName("nested message declarations by")
        class NestedMessages {

            @Test
            @DisplayName("`(is)` option")
            void markNestedTypes() {
                assertThat(Outer.Inner.class).isAssignableTo(Wrapped.class);
            }

            @Test
            @DisplayName("`(every_is)` option")
            void markEveryNested() {
                assertThat(Kindergarten.class).isAssignableTo(EducationalInstitution.class);
                assertThat(School.class).isAssignableTo(EducationalInstitution.class);
                assertThat(School.Elementary.class).isAssignableTo(EducationalInstitution.class);
                assertThat(School.HighSchool.class).isAssignableTo(EducationalInstitution.class);
                assertThat(University.class).isAssignableTo(EducationalInstitution.class);
                assertThat(University.College.class).isAssignableTo(EducationalInstitution.class);
            }
        }

        @Test
        @DisplayName("top-level message declarations as specified in `modelCompiler` settings")
        void markMessagesByFilePattern() {
            assertThat(WeatherForecast.class).isAssignableTo(DocumentMessage.class);

            // Only top-level message types should be marked.
            assertThat(WeatherForecast.Temperature.getDefaultInstance())
                    .isNotInstanceOf(DocumentMessage.class);
        }

        @Nested
        @DisplayName("a message with the interface using")
        final class MarkMessages {

            @Test
            @DisplayName("regex pattern")
            void regex() {
                assertThat(MessageEnhancedWithRegexGenerations.getDefaultInstance())
                     .isInstanceOf(RegexedMessage.class);
            }

            @Test
            @DisplayName("prefix pattern")
            void prefix() {
                assertThat(MessageEnhancedWithPrefixGenerations.getDefaultInstance())
                     .isInstanceOf(PrefixedMessage.class);
            }

            @Test
            @DisplayName("suffix pattern")
            void postfix() {
                assertThat(MessageEnhancedWithSuffixGenerations.getDefaultInstance())
                     .isInstanceOf(SuffixedMessage.class);
            }
        }
    }

    @Test
    @DisplayName("generate a custom method for a `.suffix()` pattern")
    void generateCustomPatternBasedMethod() {
        var expectedType = new MessageType(MessageEnhancedWithSuffixGenerations.getDescriptor());
        assertEquals(expectedType, MessageEnhancedWithSuffixGenerations.ownType());
    }

    @Test
    @DisplayName("generate a random UUID message")
    void generateRandomUuidMethod() {
        assertNotEquals(TypicalIdentifier.generate(), TypicalIdentifier.generate());
    }

    @Test
    @DisplayName("create instance of UUID identifier")
    void createInstanceOfUuidIdentifier() {
        var uuid = Identifier.newUuid();
        assertEquals(TypicalIdentifier.of(uuid), TypicalIdentifier.of(uuid));
    }


    @Nested
    @DisplayName("generate a custom method for a message using")
    final class GenerateMethods {

        @Test
        @DisplayName("prefix pattern")
        void prefixBasedMethod() {
            var expected = new MessageType(MessageEnhancedWithPrefixGenerations.getDescriptor());
            assertThat(MessageEnhancedWithPrefixGenerations.ownType())
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("regex pattern")
        void regexBasedMethod() {
            var expectedType = new MessageType(MessageEnhancedWithRegexGenerations.getDescriptor());
            assertThat(MessageEnhancedWithRegexGenerations.ownType())
                    .isEqualTo(expectedType);
        }

        @Test
        @DisplayName("suffix pattern")
        void suffixBasedMethod() {
            var expected = new MessageType(MessageEnhancedWithSuffixGenerations.getDescriptor());
            assertThat(MessageEnhancedWithSuffixGenerations.ownType())
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("generate a custom nested class for a message using")
    final class GenerateNestedClasses {

        @Test
        @DisplayName("prefix pattern")
        void basedOnNamePrefix() {
            Class<?> ownClass = MessageEnhancedWithPrefixGenerations.SomeNestedClass.messageClass();
            assertThat(ownClass)
                    .isEqualTo(MessageEnhancedWithPrefixGenerations.class);
        }

        @Test
        @DisplayName("regex pattern")
        void basedOnNameMatchingRegex() {
            Class<?> ownClass = MessageEnhancedWithRegexGenerations.SomeNestedClass.messageClass();
            assertThat(ownClass)
                    .isEqualTo(MessageEnhancedWithRegexGenerations.class);
        }

        @Test
        @DisplayName("suffix pattern")
        void basedOnNameSuffix() {
            Class<?> ownClass = MessageEnhancedWithSuffixGenerations.SomeNestedClass.messageClass();
            assertThat(ownClass)
                    .isEqualTo(MessageEnhancedWithSuffixGenerations.class);
        }
    }

    @Nested
    @DisplayName("generate methods using multiple factories applying")
    final class MultiFactoryGeneration {

        @Test
        @DisplayName("`UuidMethodFactory` if a message has single `uuid` field")
        void uuidMethodFactory() {
            Assertions.assertNotEquals(MFGTMessage.generate(), MFGTMessage.generate());
            var uuid = Identifier.newUuid();
            assertEquals(MFGTMessage.of(uuid), MFGTMessage.of(uuid));
        }

        @Test
        @DisplayName("a custom factory specified in the `modelCompiler/java` build settings")
        void testMethodFactory() {
            assertThat(MFGTMessage.ownType())
                    .isEqualTo(new MessageType(MFGTMessage.getDescriptor()));
        }
    }

    @Test
    @DisplayName("generate columns for a queryable entity type")
    void generateColumns() {
        EntityColumn<?, ?> column = Movie.Column.title();
        var expectedName = "title";
        assertThat(column.name().value())
                .isEqualTo(expectedName);
    }

    @Test
    @DisplayName("generate fields for a subscribable message type")
    void generateFields() {
        SubscribableField field = MovieTitleChanged.Field.oldTitle().value();
        var expectedFieldPath = "old_title.value";
        assertThat(field.getField().toString())
                .isEqualTo(expectedFieldPath);
    }

    @CanIgnoreReturnValue
    private static Class<?> checkMarkerInterface(String fqn) throws ClassNotFoundException {
        var cls = Class.forName(fqn);
        assertThat(cls.isInterface())
                .isTrue();
        assertThat(cls)
                .isAssignableTo(Message.class);

        assertThat(cls.getDeclaredMethods())
                .hasLength(0);
        return cls;
    }
}
