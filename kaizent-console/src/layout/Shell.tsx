import { AppShell, Group, NavLink, Title } from "@mantine/core";
import { NavLink as RouterNavLink, Outlet } from "react-router";
import { navigation } from "../routes";

/** The console layout: header with the product name, navigation and the current page. */
export function Shell() {
  return (
    <AppShell header={{ height: 56 }} navbar={{ width: 220, breakpoint: "sm" }} padding="md">
      <AppShell.Header>
        <Group h="100%" px="md">
          <Title order={3}>Kaizent</Title>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="xs" aria-label="Main navigation">
        {navigation.map((item) => (
          <RouterNavLink
            key={item.path}
            to={item.path}
            style={{ textDecoration: "none", color: "inherit" }}
          >
            {({ isActive }) => <NavLink component="span" label={item.label} active={isActive} />}
          </RouterNavLink>
        ))}
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
