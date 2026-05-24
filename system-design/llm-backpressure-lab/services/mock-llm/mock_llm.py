#!/usr/bin/env python3
"""Configurable Mock LLM used to simulate slow and flaky AI dependencies."""

from __future__ import annotations

import json
import os
import random
import time
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


@dataclass(frozen=True)
class MockLlmConfig:
    port: int
    min_latency_ms: int
    max_latency_ms: int
    rate_limit_percent: int
    error_percent: int
    timeout_percent: int
    seed: int | None

    @staticmethod
    def from_env() -> "MockLlmConfig":
        return MockLlmConfig(
            port=int(os.getenv("MOCK_LLM_PORT", "8088")),
            min_latency_ms=int(os.getenv("MOCK_LLM_MIN_LATENCY_MS", "2000")),
            max_latency_ms=int(os.getenv("MOCK_LLM_MAX_LATENCY_MS", "5000")),
            rate_limit_percent=int(os.getenv("MOCK_LLM_RATE_LIMIT_PERCENT", "10")),
            error_percent=int(os.getenv("MOCK_LLM_ERROR_PERCENT", "5")),
            timeout_percent=int(os.getenv("MOCK_LLM_TIMEOUT_PERCENT", "5")),
            seed=_optional_int(os.getenv("MOCK_LLM_SEED")),
        )


def _optional_int(value: str | None) -> int | None:
    return None if value in (None, "") else int(value)


def choose_outcome(config: MockLlmConfig, rng: random.Random) -> str:
    roll = rng.uniform(0, 100)
    if roll < config.rate_limit_percent:
        return "rate_limited"
    if roll < config.rate_limit_percent + config.error_percent:
        return "server_error"
    if roll < config.rate_limit_percent + config.error_percent + config.timeout_percent:
        return "timeout"
    return "success"


def latency_seconds(config: MockLlmConfig, rng: random.Random, outcome: str) -> float:
    base_ms = rng.randint(config.min_latency_ms, config.max_latency_ms)
    if outcome == "timeout":
        base_ms += config.max_latency_ms * 2
    return base_ms / 1000.0


def build_response(task_id: str | None, prompt: str | None, outcome: str, elapsed_ms: int) -> tuple[int, dict[str, Any]]:
    if outcome == "rate_limited":
        return 429, {
            "status": "rate_limited",
            "task_id": task_id,
            "error": "mock rate limit",
            "elapsed_ms": elapsed_ms,
        }
    if outcome == "server_error":
        return 500, {
            "status": "failed",
            "task_id": task_id,
            "error": "mock server error",
            "elapsed_ms": elapsed_ms,
        }
    if outcome == "timeout":
        return 504, {
            "status": "timeout",
            "task_id": task_id,
            "error": "mock timeout",
            "elapsed_ms": elapsed_ms,
        }
    return 200, {
        "status": "completed",
        "task_id": task_id,
        "summary": f"mock analysis for prompt length {len(prompt or '')}",
        "elapsed_ms": elapsed_ms,
    }


class MockLlmHandler(BaseHTTPRequestHandler):
    config = MockLlmConfig.from_env()
    rng = random.Random(config.seed)

    def do_GET(self) -> None:
        if self.path == "/health":
            self._write_json(200, {"status": "ok"})
            return
        self._write_json(404, {"error": "not found"})

    def do_POST(self) -> None:
        if self.path != "/v1/mock-completions":
            self._write_json(404, {"error": "not found"})
            return

        payload = self._read_json()
        task_id = payload.get("task_id")
        prompt = payload.get("prompt")
        outcome = choose_outcome(self.config, self.rng)
        start = time.monotonic()
        time.sleep(latency_seconds(self.config, self.rng, outcome))
        elapsed_ms = int((time.monotonic() - start) * 1000)
        status_code, body = build_response(task_id, prompt, outcome, elapsed_ms)
        self._write_json(status_code, body)

    def _read_json(self) -> dict[str, Any]:
        content_length = int(self.headers.get("content-length", "0"))
        if content_length == 0:
            return {}
        raw = self.rfile.read(content_length)
        try:
            parsed = json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            return {}
        return parsed if isinstance(parsed, dict) else {}

    def _write_json(self, status_code: int, body: dict[str, Any]) -> None:
        encoded = json.dumps(body).encode("utf-8")
        self.send_response(status_code)
        self.send_header("content-type", "application/json")
        self.send_header("content-length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, fmt: str, *args: Any) -> None:
        return


def main() -> None:
    config = MockLlmConfig.from_env()
    MockLlmHandler.config = config
    MockLlmHandler.rng = random.Random(config.seed)
    server = ThreadingHTTPServer(("0.0.0.0", config.port), MockLlmHandler)
    print(f"mock-llm listening on :{config.port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
