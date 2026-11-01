import { Stack, Text, Title } from "@mantine/core";

/** A page that a later spec fills in. */
export function PlaceholderPage({ title, spec }: { title: string; spec?: string }) {
  return (
    <Stack>
      <Title order={2}>{title}</Title>
      <Text c="dimmed">{spec ? `Coming with spec ${spec}.` : "There is nothing here."}</Text>
    </Stack>
  );
}
