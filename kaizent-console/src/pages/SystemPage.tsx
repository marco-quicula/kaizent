import { Alert, Loader, Stack, Table, Title } from "@mantine/core";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";

/** Shows `GET /api/v1/system`. Spec 0010 extends it with the workspace and the profiles. */
export function SystemPage() {
  const system = useQuery({
    queryKey: ["system"],
    queryFn: async () => {
      const { data, error } = await api.GET("/system");
      if (error || !data) {
        throw new Error("Could not load the system information.");
      }
      return data;
    },
  });

  return (
    <Stack>
      <Title order={2}>System</Title>
      {system.isPending && <Loader aria-label="Loading" />}
      {system.isError && (
        <Alert color="red" title="Error">
          {system.error.message}
        </Alert>
      )}
      {system.data && (
        <Table maw={480} variant="vertical" withTableBorder>
          <Table.Tbody>
            <Table.Tr>
              <Table.Th>Version</Table.Th>
              <Table.Td>{system.data.version}</Table.Td>
            </Table.Tr>
            <Table.Tr>
              <Table.Th>Workspace</Table.Th>
              <Table.Td>{system.data.workspace ?? "Not configured"}</Table.Td>
            </Table.Tr>
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  );
}
