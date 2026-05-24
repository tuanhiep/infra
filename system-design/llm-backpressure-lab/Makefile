.PHONY: test mock-llm smoke-mock-llm up down logs

MOCK_LLM_PORT ?= 8088

test:
	python3 -m unittest discover -s services/mock-llm/tests

mock-llm:
	MOCK_LLM_PORT=$(MOCK_LLM_PORT) python3 services/mock-llm/mock_llm.py

smoke-mock-llm:
	curl -sS -X POST "http://localhost:$(MOCK_LLM_PORT)/v1/mock-completions" \
		-H "content-type: application/json" \
		-d '{"task_id":"smoke-test","prompt":"explain backpressure"}'

up:
	docker compose up --build

down:
	docker compose down --remove-orphans

logs:
	docker compose logs -f
