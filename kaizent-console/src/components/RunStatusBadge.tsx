import { Badge, type MantineColor } from "@mantine/core";
import type { RunStatus } from "../api/types";
import { statusLabel } from "../format";

const colors: Record<RunStatus, MantineColor> = {
  RECEIVED: "gray",
  TRIAGING: "blue",
  RESOLVING: "blue",
  AWAITING_REVIEW: "yellow",
  APPROVED: "teal",
  DELIVERED: "green",
  REJECTED: "orange",
  FAILED: "red",
  EVALUATED: "violet",
};

/** A run status, for example "Awaiting review". */
export function RunStatusBadge({ status }: { status: RunStatus }) {
  return (
    <Badge color={colors[status]} variant="light">
      {statusLabel(status)}
    </Badge>
  );
}
