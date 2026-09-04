# Unit Test Creation Skill (SKILL.md)

## Overview

This skill enables systematic creation of high-quality unit tests following 
Behavior-Driven Development (BDD) patterns. It's designed for:

- **Java developers** building Spring Boot applications
- **Integration with Claude Code** for AI-assisted test generation
- **Team consistency** across test writing practices
- **Automated test generation** from method analysis

**Skill Level**: Intermediate to Advanced
**Framework**: JUnit 5 (Jupiter) + Mockito + AssertJ
**Pattern**: Given-When-Then (Gherkin-style BDD)

---

## When to Use This Skill

✅ **USE when:**
- Creating unit tests for new features
- Adding test coverage to legacy code
- Generating tests with Claude Code
- Establishing testing standards for your team
- Writing tests for business logic (services, repositories)

❌ **DON'T use for:**
- Integration tests (use integration-test skill instead)
- UI/Controller testing (limited to business logic)
- Load testing or performance testing
- End-to-end testing scenarios

---

## The 4-Step Test Creation Process

### Step 1: Analyze the Method Under Test

Before writing any test, understand:
- Input parameters and their types
- Return type and possible outcomes
- External dependencies (repositories, services, APIs)
- Validation rules and constraints
- Error conditions and exceptions
- Business logic pathways

**Example Analysis:**

```
Method: PaymentService.process(Account account, Payment payment)

Inputs:
  - Account: account ID, balance, status
  - Payment: payment ID, amount, type

Dependencies:
  - PaymentRepository (save payment records)
  - BankGateway (charge external bank)
  - NotificationService (send emails)

Return: PaymentResult (success/failure with reason code)

Error Conditions:
  - Insufficient funds
  - Account suspended
  - Invalid amount
  - External gateway timeout
```

### Step 2: Map Test Scenarios

List ALL scenarios your method should handle:

```
Happy Path:
  - Process payment successfully with sufficient funds
  - Apply loyalty rewards for platinum customers
  - Send confirmation notification

Failure Cases:
  - Reject payment: insufficient funds
  - Reject payment: account suspended
  - Reject payment: amount exceeds daily limit

Edge Cases:
  - Zero amount payment
  - Negative amount payment
  - Maximum decimal precision
  - Concurrent payment attempts (race condition)

Exception Handling:
  - Bank gateway timeout
  - Database connection error
  - Email service failure
```

### Step 3: Design Test Organization

Structure your test class for clarity:

```
class PaymentServiceTest
├── Setup (@BeforeEach)
├── SuccessfulPaymentTests (@Nested)
│   ├── shouldProcess_WhenFundsAvailable
│   └── shouldApplyRewards_WhenPlatinumCustomer
├── PaymentRejectionTests (@Nested)
│   ├── shouldReject_WhenInsufficientFunds
│   └── shouldReject_WhenAccountSuspended
└── EdgeCaseTests (@Nested)
    ├── shouldHandleZeroAmount
    └── shouldHandleMaxDecimalPrecision
```

### Step 4: Implement & Validate

Write tests and verify:
- ✅ Code coverage (90%+ for business logic)
- ✅ All scenarios are tested
- ✅ Assertions are specific (not just assertTrue)
- ✅ Mocks are used correctly (only for dependencies)
- ✅ Test names describe behavior, not implementation
- ✅ Tests run independently (no order dependency)

---

## Template: Standard Test Method

```java
@Test
@DisplayName("should [BEHAVIOR] when [CONDITION] and [OPTIONAL_CONDITION]")
void shouldDescribeBehavior() {
    // GIVEN: Set up initial state
    // - Create test data
    // - Configure mocks
    // - Establish preconditions
    var input = new PaymentRequest("PAY001", BigDecimal.valueOf(500));
    when(accountRepository.findById("ACC001"))
        .thenReturn(Optional.of(validAccount));
    
    // WHEN: Execute the action being tested
    // - Call the method under test
    // - Capture the result
    var result = paymentService.process(validAccount, input);
    
    // THEN: Assert the expected outcome
    // - Verify the result
    // - Verify mock interactions (if applicable)
    // - Check state changes
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getTransactionId()).isNotNull();
    verify(paymentRepository).save(any());
}
```

---

## Common Test Scenarios & Examples

### Scenario 1: Success Case

```java
@Test
@DisplayName("should process payment when amount is valid and account has sufficient funds")
void shouldProcessPaymentWithSufficientFunds() {
    // GIVEN: Account with $1000, payment request for $500
    var account = new BankAccount("ACC001", BigDecimal.valueOf(1000));
    var payment = new Payment("PAY001", BigDecimal.valueOf(500));
    
    // WHEN: Processing the payment
    var result = paymentService.process(account, payment);
    
    // THEN: Payment should succeed and balance updated
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getTransactionId()).isNotNull();
    assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(500));
}
```

### Scenario 2: Failure Case (with assertions for the failure reason)

```java
@Test
@DisplayName("should reject payment when account has insufficient funds")
void shouldRejectPaymentWhenInsufficientFunds() {
    // GIVEN: Account with only $100, payment request for $500
    var account = new BankAccount("ACC001", BigDecimal.valueOf(100));
    var payment = new Payment("PAY001", BigDecimal.valueOf(500));
    
    // WHEN: Processing the payment
    var result = paymentService.process(account, payment);
    
    // THEN: Payment should be rejected with specific reason
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
    assertThat(result.getMessage()).contains("$500 exceeds available balance");
}
```

### Scenario 3: Mock Verification (external dependency)

```java
@Test
@DisplayName("should send confirmation email when payment succeeds")
void shouldSendConfirmationEmailOnSuccess() {
    // GIVEN: Valid payment request and mocked email service
    var account = new BankAccount("ACC001", BigDecimal.valueOf(1000));
    var payment = new Payment("PAY001", BigDecimal.valueOf(500));
    var emailService = mock(EmailService.class);
    var paymentService = new PaymentService(emailService);
    
    // WHEN: Processing payment
    paymentService.process(account, payment);
    
    // THEN: Email service should be called with correct recipient
    verify(emailService).send(
        argThat(email -> 
            email.getRecipient().equals("customer@example.com") &&
            email.getSubject().contains("Payment Confirmation")
        )
    );
}
```

### Scenario 4: Parameterized Tests (multiple inputs)

```java
@ParameterizedTest
@CsvSource({
    "1000, 500, true",   // balance, payment, shouldSucceed
    "1000, 1000, true",  // exact balance
    "1000, 1001, false", // exceeds balance
    "0, 1, false",       // zero balance
})
@DisplayName("should determine payment viability based on account balance")
void shouldValidatePaymentAgainstBalance(
    BigDecimal balance, 
    BigDecimal paymentAmount, 
    boolean expectedSuccess) {
    
    // GIVEN
    var account = new BankAccount("ACC001", balance);
    var payment = new Payment("PAY001", paymentAmount);
    
    // WHEN
    var result = paymentService.canProcess(account, payment);
    
    // THEN
    assertThat(result).isEqualTo(expectedSuccess);
}
```

---

## Organizing with @Nested Classes

Structure related tests for readability:

```java
@Nested
@DisplayName("Successful Payment Processing")
class SuccessfulPaymentTests {
    
    @Test
    @DisplayName("should process standard payment")
    void test1() { }
    
    @Test
    @DisplayName("should apply loyalty rewards")
    void test2() { }
}

@Nested
@DisplayName("Payment Rejection Cases")
class PaymentRejectionTests {
    
    @Test
    @DisplayName("should reject: insufficient funds")
    void test1() { }
    
    @Test
    @DisplayName("should reject: account suspended")
    void test2() { }
}
```

---

## Mock & Verify Patterns

### Mock Only Dependencies (Not the Class Under Test)

✅ CORRECT:
```java
paymentRepository = mock(PaymentRepository.class);
paymentService = new PaymentService(paymentRepository);
```

❌ WRONG:
```java
paymentService = mock(PaymentService.class); // Don't mock the class you're testing!
```

### Verify External Calls

```java
// Verify the method was called
verify(emailService).send(any());

// Verify the method was called with specific argument
verify(emailService).send(
    argThat(email -> email.getRecipient().equals("user@example.com"))
);

// Verify the method was NOT called
verify(emailService, never()).send(any());

// Verify the method was called exactly once
verify(emailService, times(1)).send(any());
```

---

## Best Practices (DO's)

✅ DO:
- Use @DisplayName for clear, human-readable test descriptions
- Start test names with "should" (describes expected behavior)
- Use Given-When-Then comments to structure logic
- Test ONE behavior per test method
- Use @Nested classes to organize by scenario type
- Use @ParameterizedTest for similar tests with different data
- Use AssertJ fluent API (assertThat instead of assertEquals)
- Keep test data minimal and meaningful (not random values)
- Mock only external dependencies
- Verify mocks only for important side effects
- Make tests independent (can run in any order)
- Use meaningful variable names that document the test

---

## Common Mistakes (DON'Ts)

❌ DON'T:
- Use cryptic test names (testPayment(), test1(), etc.)
- Test multiple behaviors in one test
- Create complex test data builders that obscure the test purpose
- Use assertTrue/assertFalse without context
- Mock the class under test
- Mock simple objects like String or Integer
- Leave test data setup in the test method (use @BeforeEach for shared setup)
- Test implementation details instead of behavior
- Create tests that depend on execution order
- Skip @DisplayName — method names are not sufficient documentation
- Use generic assertion messages (provide context with assertThat)

---

## Integration with Claude Code

When using Claude Code to generate tests, provide this prompt structure:

```
Generate unit tests for [ClassName].[methodName]() with:

1. Naming convention:
   - @DisplayName("should [behavior] when [condition]")
   - Method name: shouldDescribeBehavior()

2. Structure: Every test MUST use Given-When-Then:
   // GIVEN: [setup]
   // WHEN: [action]  
   // THEN: [assertion]

3. Test scenarios to cover:
   - [Success scenario 1]
   - [Failure scenario 1]
   - [Edge case 1]

4. Mocking:
   - Mock: [list external dependencies]
   - Don't mock: [the class being tested]

5. Assertions:
   - Use AssertJ (assertThat)
   - Assert behavior, not just success/failure
   - Verify mocks where relevant

6. Follow agent.md standards for:
   - Test organization with @Nested classes
   - Code coverage targets (90%+ for business logic)
   - One behavior per test
```

---

## Code Coverage Targets

By component type:

| Component Type                            | Target |
|-------------------------------------------|--------| 
| Domain Models (Entity, Value Objects)     | 90%+   |
| Services (Business Logic)                 | 85%+   |
| Controllers (Request Handling)            | 70%+   |
| Repositories (Data Access)                | 80%+   |
| Configuration Classes                     | 50%+   |
| **Overall Project**                       | 75%+   |

Enforce in CI/CD: Block PRs below 70% overall coverage.
Measure with: JaCoCo + SonarQube

---

## Tools & Dependencies

Required for this skill:

```xml
<!-- Test Framework -->
&lt;dependency&gt;
    &lt;groupId&gt;org.junit.jupiter&lt;/groupId&gt;
    &lt;artifactId&gt;junit-jupiter&lt;/artifactId&gt;
    &lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;

<!-- Mocking Framework -->
&lt;dependency&gt;
    &lt;groupId&gt;org.mockito&lt;/groupId&gt;
    &lt;artifactId&gt;mockito-core&lt;/artifactId&gt;
    &lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;

<!-- Assertions Library -->
&lt;dependency&gt;
    &lt;groupId&gt;org.assertj&lt;/groupId&gt;
    &lt;artifactId&gt;assertj-core&lt;/artifactId&gt;
    &lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;

<!-- Code Coverage -->
&lt;dependency&gt;
    &lt;groupId&gt;org.jacoco&lt;/groupId&gt;
    &lt;artifactId&gt;jacoco-maven-plugin&lt;/artifactId&gt;
    &lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;
```

---

## Related Documentation

- **agent.md** - Testing standards and BDD patterns
- **README.md** - Link to this skill in your project
- **CONTRIBUTING.md** - Reference this skill for test creation
- **JUnit 5 Docs** - https://junit.org/junit5/docs
- **Mockito Docs** - https://javadoc.io/doc/org.mockito/mockito-core
- **AssertJ Docs** - https://assertj.github.io/assertj-core

---

**Last Updated**: June 2026
**Version**: 1.0
**Maintainer**: Development Team
**Status**: Production Ready
