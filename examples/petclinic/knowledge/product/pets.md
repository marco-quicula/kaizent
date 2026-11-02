# Pets

Each pet belongs to one owner. The owner's page lists the owner's pets and their visits.

## Pet fields and rules

| Field | Rule |
|---|---|
| Name | Required, up to 30 characters, and **unique per owner**: an owner cannot have two pets with the same name, whatever the upper or lower case. The message is "already exists" |
| Birth Date | Required, in the format `yyyy-MM-dd`, and **not in the future** (see KI-002) |
| Type | Required when the pet is added; chosen from the list of pet types |

Two different owners can each have a pet with the same name.

## Add a pet

1. Open the owner's page.
2. Click **Add New Pet**.
3. Fill in **Name**, **Birth Date** and **Type**, and click **Add Pet**.
4. The owner's page opens with the message "New Pet has been Added".

## Edit a pet

On the owner's page, click **Edit Pet** next to the pet, change the fields and click
**Update Pet**. The same rules apply.

## Pet types

The list of types is reference data: cat, dog, lizard, snake, bird and hamster. **There is no
screen to add, rename or remove a pet type.** A new type, for example "rabbit", must be added
to the database by an administrator, who inserts a row into the `types` table. In the
default in-memory database, the data is reloaded at every start, so the change must also be
added to the database's data script to survive a restart.
