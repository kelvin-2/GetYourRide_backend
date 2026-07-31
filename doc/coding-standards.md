# Coding Standards

## General

- Write production-quality code.
- Prefer readability over cleverness.
- Keep methods short.
- Use meaningful names.

## Spring

- Constructor injection only.
- Business logic belongs in Services.
- Controllers should orchestrate only.
- Repositories only access data.

## Validation

Always validate incoming DTOs.

Use:

- @Valid
- Bean Validation
- Custom validators when necessary

Never trust client input.

## Database

- Never hardcode IDs.
- Never break existing migrations.
- Never remove columns without approval.
- Maintain referential integrity.

## Security

- Never commit secrets.
- Use environment variables.
- Respect authentication.
- Respect authorization.

## Git

- One feature per branch.
- Keep commits focused.
- Avoid mixing unrelated changes.