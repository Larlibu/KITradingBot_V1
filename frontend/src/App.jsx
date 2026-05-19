import AppView from "./components/AppView";
import { useAppLogic } from "./hooks/useAppLogic";

/**
 * Schlanker App-Einstiegspunkt:
 * - Controller-Logik kommt aus useAppLogic
 * - Rendering übernimmt AppView
 */
export default function App() {
  const viewModel = useAppLogic();
  return <AppView {...viewModel} />;
}
