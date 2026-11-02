import { Alert, Anchor, Button, Card, Group, Loader, Stack, Text, Title } from "@mantine/core";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router";
import { api } from "../api/client";
import type { RunDetail } from "../api/types";
import { RunStatusBadge } from "../components/RunStatusBadge";
import { formatDateTime } from "../format";
import { RUNS_REFRESH_MS } from "./RunsPage";

/** A run: the problem, the proposal, and the Approve button while it awaits review. */
export function RunDetailPage() {
  const { id = "" } = useParams();
  const queryClient = useQueryClient();
  const run = useQuery({
    queryKey: ["run", id],
    queryFn: async () => {
      const { data, error } = await api.GET("/runs/{id}", { params: { path: { id } } });
      if (error || !data) {
        throw new Error(error?.detail ?? "Could not load the run.");
      }
      return data;
    },
    refetchInterval: RUNS_REFRESH_MS,
  });
  const approve = useMutation({
    mutationFn: async () => {
      const { data, error } = await api.POST("/runs/{id}/verdict", {
        params: { path: { id } },
        body: { type: "APPROVED" },
      });
      if (error || !data) {
        throw new Error(error?.detail ?? "Could not approve the run.");
      }
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(["run", id], updated);
      return queryClient.invalidateQueries({ queryKey: ["runs"] });
    },
  });

  return (
    <Stack>
      <Anchor component={Link} to="/runs" size="sm">
        ← Runs
      </Anchor>
      {run.isPending && <Loader aria-label="Loading" />}
      {run.isError && (
        <Alert color="red" title="Error">
          {run.error.message}
        </Alert>
      )}
      {run.data && (
        <RunView
          run={run.data}
          onApprove={() => approve.mutate()}
          approving={approve.isPending}
          approveError={approve.error?.message}
        />
      )}
    </Stack>
  );
}

function RunView({
  run,
  onApprove,
  approving,
  approveError,
}: {
  run: RunDetail;
  onApprove: () => void;
  approving: boolean;
  approveError?: string;
}) {
  const { problem, proposal, verdict, failure } = run;
  return (
    <Stack>
      <Group justify="space-between" align="flex-start">
        <Stack gap={4}>
          <Title order={2}>
            {problem.ticketKey}: {problem.title}
          </Title>
          <Text size="sm" c="dimmed">
            Received {formatDateTime(problem.receivedAt)} from {problem.source}
            {problem.requester ? `, requested by ${problem.requester}` : ""}
          </Text>
        </Stack>
        <RunStatusBadge status={run.status} />
      </Group>

      {failure && (
        <Alert color="red" title={`Failed (${failure.reason})`}>
          {failure.message}
        </Alert>
      )}

      <Card withBorder>
        <Title order={4} mb="xs">
          Problem
        </Title>
        <Text style={{ whiteSpace: "pre-wrap" }}>{problem.body}</Text>
      </Card>

      {proposal && (
        <Card withBorder>
          <Group justify="space-between" mb="xs">
            <Title order={4}>Proposal</Title>
            <Text size="sm" c="dimmed">
              Confidence {Math.round(proposal.rawConfidence * 100)}%
            </Text>
          </Group>
          <Title order={5}>Diagnosis</Title>
          <Text mb="sm" style={{ whiteSpace: "pre-wrap" }}>
            {proposal.diagnosis}
          </Text>
          <Title order={5}>Suggested reply</Title>
          <Text style={{ whiteSpace: "pre-wrap" }}>{proposal.suggestedReply}</Text>
        </Card>
      )}

      {verdict && (
        <Text size="sm">
          {verdict.type === "APPROVED" ? "Approved" : verdict.type} by {verdict.reviewer ?? "?"} on{" "}
          {formatDateTime(verdict.reviewedAt)}.
        </Text>
      )}

      {approveError && (
        <Alert color="red" title="Approval failed">
          {approveError}
        </Alert>
      )}

      {run.status === "AWAITING_REVIEW" && (
        <Group>
          <Button onClick={onApprove} loading={approving}>
            Approve
          </Button>
        </Group>
      )}
    </Stack>
  );
}
