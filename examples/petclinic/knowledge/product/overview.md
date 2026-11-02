# PetClinic overview

PetClinic is a web application that a veterinary clinic uses to manage **owners**, their
**pets** and the pets' **visits**, and to list the clinic's **veterinarians**.

This knowledge base describes
[Spring PetClinic](https://github.com/spring-projects/spring-petclinic) at commit
`500158f732419217507c7656904b8e6aa1bcc0d6` (2026-09-29). It was written for the Kaizent demo
and is the source of truth for the demo's answers.

## Main menu

| Menu item | Opens |
|---|---|
| **Home** | The welcome page (`/`) |
| **Find Owners** | The owner search (`/owners/find`) |
| **Veterinarians** | The list of veterinarians (`/vets.html`) |
| **Error** | A page that triggers an error on purpose (see KI-003) |

## Where to find what

- Owners: [owners.md](owners.md)
- Pets and pet types: [pets.md](pets.md)
- Visits: [visits.md](visits.md)
- Veterinarians: [veterinarians.md](veterinarians.md)
- Common questions: [../faq.md](../faq.md)
- Running and configuring the application: [../setup/running-locally.md](../setup/running-locally.md)
  and [../setup/databases.md](../setup/databases.md)

## What PetClinic does not do

PetClinic has no user accounts or permissions, no billing, no appointment calendar, and no
screens to manage reference data such as pet types or veterinary specialties.
