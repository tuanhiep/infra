import importlib.util
from pathlib import Path
import random
import sys
import unittest


MODULE_PATH = Path(__file__).resolve().parents[1] / "mock_llm.py"
SPEC = importlib.util.spec_from_file_location("mock_llm", MODULE_PATH)
mock_llm = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules["mock_llm"] = mock_llm
SPEC.loader.exec_module(mock_llm)


class MockLlmTest(unittest.TestCase):
    def test_success_response_contains_task_and_elapsed_time(self):
        status, body = mock_llm.build_response("task-1", "hello", "success", 2500)

        self.assertEqual(200, status)
        self.assertEqual("completed", body["status"])
        self.assertEqual("task-1", body["task_id"])
        self.assertEqual(2500, body["elapsed_ms"])

    def test_error_responses_use_retryable_status_codes(self):
        cases = {
            "rate_limited": 429,
            "server_error": 500,
            "timeout": 504,
        }

        for outcome, expected_status in cases.items():
            with self.subTest(outcome=outcome):
                status, body = mock_llm.build_response("task-2", "hello", outcome, 3000)
                self.assertEqual(expected_status, status)
                self.assertEqual("task-2", body["task_id"])

    def test_latency_increases_for_timeout_outcome(self):
        config = mock_llm.MockLlmConfig(
            port=8088,
            min_latency_ms=100,
            max_latency_ms=100,
            rate_limit_percent=0,
            error_percent=0,
            timeout_percent=0,
            seed=7,
        )

        normal = mock_llm.latency_seconds(config, random.Random(7), "success")
        timeout = mock_llm.latency_seconds(config, random.Random(7), "timeout")

        self.assertEqual(0.1, normal)
        self.assertGreater(timeout, normal)

    def test_outcome_selection_respects_zero_failure_config(self):
        config = mock_llm.MockLlmConfig(
            port=8088,
            min_latency_ms=100,
            max_latency_ms=200,
            rate_limit_percent=0,
            error_percent=0,
            timeout_percent=0,
            seed=11,
        )

        outcomes = {mock_llm.choose_outcome(config, random.Random(i)) for i in range(20)}

        self.assertEqual({"success"}, outcomes)


if __name__ == "__main__":
    unittest.main()
