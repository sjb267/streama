# audio_moderation_service.py
import hashlib
import json
import numpy as np
import torch
import webrtcvad
import torch.nn.functional as F
import whisper
from pathlib import Path
import warnings
import ffmpeg
import tensorflow as tf  # 添加这行
import tensorflow_hub as hub  # 添加这行
import cv2
import os
import platform
import re
import shutil
import uuid
from PIL import Image
import wave
import contextlib
from datetime import datetime
warnings.filterwarnings('ignore')

class VideoModerationService:
    STABLE_RISK_TYPES = {
        "normal",
        "violence",
        "sexual",
        "political",
        "fraud",
        "gambling",
        "drug",
        "minor_safety",
        "self_harm",
        "privacy",
        "hate_harassment",
        "terror_extremism",
        "vulgar",
        "animal_cruelty",
        "copyright",
        "illegal_trade",
        "other",
        "audio_event",
        "audit_incomplete",
    }

    ZERO_TOLERANCE_RISK_TYPES = {
        "fraud",
        "gambling",
        "drug",
        "minor_safety",
        "self_harm",
        "privacy",
        "terror_extremism",
        "illegal_trade",
    }

    REVIEW_ONLY_AUDIO_CATEGORIES = {
        "gunshot",
        "explosion",
        "scream",
        "glass_break",
        "alarm",
        "fight",
        "sexual_moan",
        "distress",
        "riot",
        "police_siren",
    }

    TEXT_POLICY_RULES = [
        {
            "risk_type": "fraud",
            "risk_subtype": "scam_or_diversion",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "刷单", "返利", "杀猪盘", "兼职日结", "贷款秒批", "免费提现", "扫码领取",
                "加微信赚钱", "vx赚钱", "投资群", "高收益", "稳赚", "保证收益",
                "crypto giveaway", "telegram投资", "whatsapp赚钱", "airdrop scam",
            ],
        },
        {
            "risk_type": "fraud",
            "risk_subtype": "contact_diversion",
            "severity": "medium",
            "policy_action": "review",
            "keywords": [
                "加微信", "微信号", "vx", "v信", "qq号", "私聊", "进群", "群号",
                "扫码进群", "二维码", "telegram", "whatsapp", "line私聊",
            ],
        },
        {
            "risk_type": "gambling",
            "risk_subtype": "gambling_promotion",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "博彩", "赌博", "下注", "盘口", "赔率", "百家乐", "老虎机", "棋牌投注",
                "现金网", "娱乐城", "彩票代投", "体育投注", "casino", "betting",
            ],
        },
        {
            "risk_type": "drug",
            "risk_subtype": "drug_trade_or_instruction",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "毒品", "冰毒", "大麻", "摇头丸", "k粉", "可卡因", "海洛因",
                "卖药", "迷幻蘑菇", "麻古", "lsd", "cocaine", "weed dealer",
            ],
        },
        {
            "risk_type": "minor_safety",
            "risk_subtype": "minor_sexual_or_grooming",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "未成年裸", "萝莉福利", "幼女", "小学生约", "未成年人约",
                "child porn", "cp资源", "loli hentai", "未成年私密",
            ],
        },
        {
            "risk_type": "self_harm",
            "risk_subtype": "self_harm_instruction",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "自杀教程", "割腕方法", "怎么自杀", "安乐死方法", "轻生教程",
                "suicide method", "how to self harm", "kill myself tutorial",
            ],
        },
        {
            "risk_type": "self_harm",
            "risk_subtype": "self_harm_expression",
            "severity": "medium",
            "policy_action": "review",
            "keywords": ["想自杀", "不想活了", "我要轻生", "割腕", "自残", "suicidal", "self harm"],
        },
        {
            "risk_type": "privacy",
            "risk_subtype": "personal_information_leak",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "身份证号", "手机号", "家庭住址", "银行卡号", "开盒", "人肉",
                "dox", "doxxing", "home address", "phone number leak",
            ],
        },
        {
            "risk_type": "hate_harassment",
            "risk_subtype": "hate_or_abuse",
            "severity": "medium",
            "policy_action": "review",
            "keywords": [
                "去死", "废物", "人身攻击", "地域黑", "种族歧视", "性别歧视",
                "hate speech", "racial slur", "harassment",
            ],
        },
        {
            "risk_type": "terror_extremism",
            "risk_subtype": "extremism_or_terror",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "恐怖组织", "极端组织", "圣战", "炸弹教程", "爆炸物制作",
                "terrorist", "isis", "extremist", "make a bomb",
            ],
        },
        {
            "risk_type": "vulgar",
            "risk_subtype": "vulgar_or_soft_porn",
            "severity": "medium",
            "policy_action": "review",
            "keywords": [
                "擦边", "福利姬", "裸聊", "大尺度", "成人内容", "约炮",
                "onlyfans", "nsfw", "soft porn",
            ],
        },
        {
            "risk_type": "animal_cruelty",
            "risk_subtype": "animal_abuse",
            "severity": "high",
            "policy_action": "reject",
            "keywords": ["虐猫", "虐狗", "虐待动物", "animal abuse", "animal cruelty"],
        },
        {
            "risk_type": "copyright",
            "risk_subtype": "piracy_or_reupload",
            "severity": "medium",
            "policy_action": "review",
            "keywords": ["盗版全集", "网盘资源", "无授权转载", "全集下载", "pirated", "full movie download"],
        },
        {
            "risk_type": "illegal_trade",
            "risk_subtype": "illegal_goods_trade",
            "severity": "high",
            "policy_action": "reject",
            "keywords": [
                "买枪", "卖枪", "枪支出售", "管制刀具", "假证", "代开发票",
                "sell gun", "fake id", "illegal trade",
            ],
        },
    ]

    FICTIONAL_CONTEXT_KEYWORDS = [
        "游戏", "手游", "端游", "玩家", "第一人称", "fps", "game", "gameplay", "hud",
        "电影", "影视", "剪辑", "剧集", "电视剧", "片段", "movie", "film", "drama",
        "动漫", "动画", "漫画", "二次元", "anime", "animation", "cosplay",
    ]

    HARD_VIOLENCE_KEYWORDS = [
        "真实伤害", "真实暴力", "流血", "血腥", "血浆", "尸体", "死亡", "杀死", "砍死",
        "爆头", "处决", "虐杀", "教程", "制作炸弹", "自制枪", "威胁", "恐吓", "教唆",
        "blood", "gore", "corpse", "dead body", "murder", "kill", "execution",
        "how to", "tutorial", "threat", "real violence",
    ]

    def __init__(
        self,
        model_dir="./models",
        device='cpu',
        qwen_device='cuda',
        trace_model_inputs=True,
        trace_dir="./audit_results/model_input_traces",
    ):
        """
        初始化视频审核服务
        Args:
            model_dir: 模型存储目录
            device: 运行设备 ('cpu' 或 'cuda')
        """
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(parents=True, exist_ok=True)
        
        # 设置设备
        self.device = torch.device(device if torch.cuda.is_available() and device == 'cuda' else 'cpu')
        print(f"使用设备: {self.device}")
        
        # 模型实例
        self.cpu_device = torch.device('cpu')
        self.device = self.cpu_device
        self.qwen_target_device = self._normalize_device_name(qwen_device or device or 'cuda')
        self.qwen_runtime_device = None
        self.model_errors = {
            "yamnet": None,
            "whisper": None,
            "qwen": None,
            "vad": None,
        }
        self.runtime_checks = {}
        print(f"Using CPU device for YAMNet/Whisper/VAD: {self.cpu_device}")
        print(f"Qwen target device: {self.qwen_target_device}")

        self.yamnet_model = None
        self.yamnet_class_names = None
        self.whisper_model = None
        self.qwen_model = None
        self.qwen_processor = None
        self.yamnet_interpreter = None
        self.trace_model_inputs = bool(trace_model_inputs)
        self.trace_dir = Path(trace_dir)
        self.trace_dir.mkdir(parents=True, exist_ok=True)
        self._trace_session_dir = None
        self._trace_session_id = None
        self._trace_files = []

        # VAD配置
        self.vad = None
        self.vad_mode = 2  # VAD灵敏度 (0-3, 3最敏感)
        self.vad_frame_duration = 30  # 帧时长（毫秒）
        self.vad_sample_rate = 16000  # VAD采样率
        self.vad_frame_size = int(self.vad_sample_rate * self.vad_frame_duration / 1000)  # 每帧样本数

        #yamnet配置
        self.yamnet_sample_rate = 16000  # YAMNet要求的采样率
        self.yamnet_frame_hop_seconds = 0.48  # YAMNet score frame hop in seconds

        #帧提取配置
        self.keyframes_dir = Path("./keyframes")
        self.keyframes_dir.mkdir(exist_ok=True)
        self.frame_extraction_rate = 1  # 每秒提取1帧

        # 审核配置
        self.risk_thresholds = {
            "high": 0.3,    # 高风险 <0.3
            "medium": 0.6,  # 中风险 0.3-0.6
            "low": 0.6      # 低风险 >0.6
        }

        # 审核结果存储
        self.audit_results_dir = Path("./audit_results")
        self.audit_results_dir.mkdir(exist_ok=True)

    @staticmethod
    def _normalize_device_name(device):
        raw = str(device or "").strip().lower()
        if raw == "gpu":
            return "cuda"
        if raw.startswith("cuda"):
            return raw
        return "cpu"

    @staticmethod
    def _sanitize_trace_part(value):
        text = str(value or "unknown").strip()
        text = re.sub(r"[^A-Za-z0-9_.-]+", "_", text)
        text = text.strip("._")
        return text[:80] or "unknown"

    @classmethod
    def _json_safe(cls, value):
        if value is None or isinstance(value, (str, int, float, bool)):
            return value
        if isinstance(value, Path):
            return str(value)
        if isinstance(value, np.generic):
            return value.item()
        if isinstance(value, np.ndarray):
            return value.tolist()
        if isinstance(value, dict):
            return {str(key): cls._json_safe(item) for key, item in value.items()}
        if isinstance(value, (list, tuple, set)):
            return [cls._json_safe(item) for item in value]
        return str(value)

    @staticmethod
    def _float_or_none(value):
        try:
            return float(value)
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _is_cjk_char(value):
        if not value:
            return False
        codepoint = ord(value[0])
        return (
            0x4E00 <= codepoint <= 0x9FFF
            or 0x3400 <= codepoint <= 0x4DBF
            or 0x3040 <= codepoint <= 0x30FF
            or 0xAC00 <= codepoint <= 0xD7AF
        )

    @classmethod
    def _join_word_text(cls, words):
        pieces = []
        previous = ""
        closing_punctuation = set(",.!?;:，。！？；：、)]}）】》”’")
        for word in words or []:
            raw = str(word.get("word") or word.get("text") or "")
            if not raw:
                continue
            if pieces and not raw[0].isspace():
                previous_char = previous[-1:] if previous else ""
                current_char = raw[:1]
                if (
                    current_char not in closing_punctuation
                    and not cls._is_cjk_char(previous_char)
                    and not cls._is_cjk_char(current_char)
                ):
                    pieces.append(" ")
            pieces.append(raw)
            previous = raw
        return re.sub(r"\s+", " ", "".join(pieces)).strip()

    @classmethod
    def _normalize_whisper_segments(cls, raw_segments, offset_seconds):
        normalized_segments = []
        normalized_words = []
        offset = float(offset_seconds or 0.0)

        for raw_segment in raw_segments or []:
            if not isinstance(raw_segment, dict):
                continue
            segment_start = cls._float_or_none(raw_segment.get("start"))
            segment_end = cls._float_or_none(raw_segment.get("end"))
            normalized_segment = {
                "start": round(offset + segment_start, 3) if segment_start is not None else None,
                "end": round(offset + segment_end, 3) if segment_end is not None else None,
                "text": str(raw_segment.get("text") or "").strip(),
            }
            segment_words = []
            for raw_word in raw_segment.get("words") or []:
                if not isinstance(raw_word, dict):
                    continue
                word_text = str(raw_word.get("word") or raw_word.get("text") or "")
                word_start = cls._float_or_none(raw_word.get("start"))
                word_end = cls._float_or_none(raw_word.get("end"))
                if not word_text and word_start is None and word_end is None:
                    continue
                normalized_word = {
                    "word": word_text,
                    "start": round(offset + word_start, 3) if word_start is not None else None,
                    "end": round(offset + word_end, 3) if word_end is not None else None,
                }
                if raw_word.get("probability") is not None:
                    normalized_word["probability"] = cls._float_or_none(raw_word.get("probability"))
                segment_words.append(normalized_word)
                normalized_words.append(normalized_word)
            normalized_segment["words"] = segment_words
            normalized_segments.append(normalized_segment)

        return normalized_segments, normalized_words

    @classmethod
    def _words_for_window(cls, words, window_start, window_end):
        selected = []
        start = float(window_start)
        end = float(window_end)
        for word in words or []:
            if not isinstance(word, dict):
                continue
            word_start = cls._float_or_none(word.get("start"))
            word_end = cls._float_or_none(word.get("end"))
            if word_start is None and word_end is None:
                continue
            if word_start is None:
                word_start = word_end
            if word_end is None:
                word_end = word_start
            if word_end <= start or word_start >= end:
                continue
            selected.append(word)
        return selected

    def begin_model_input_trace_session(self, video_path=None):
        self._trace_files = []
        self._trace_session_dir = None
        self._trace_session_id = None
        if not self.trace_model_inputs:
            return None
        source = str(video_path or "unknown_video")
        digest_source = source
        try:
            source_path = Path(source)
            if source_path.exists():
                stat = source_path.stat()
                digest_source = f"{source_path.resolve()}|{stat.st_size}|{stat.st_mtime_ns}"
        except Exception:
            pass
        video_hash = hashlib.sha1(digest_source.encode("utf-8", errors="ignore")).hexdigest()[:10]
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        self._trace_session_id = f"{timestamp}_{video_hash}"
        self._trace_session_dir = self.trace_dir / self._trace_session_id
        (self._trace_session_dir / "images").mkdir(parents=True, exist_ok=True)
        return self._trace_session_dir

    def _ensure_model_input_trace_session(self):
        if not self.trace_model_inputs:
            return None
        if self._trace_session_dir is None:
            return self.begin_model_input_trace_session("unknown_video")
        return self._trace_session_dir

    def _build_trace_base_name(self, segment):
        segment_id = self._sanitize_trace_part(segment.get("segment_id"))
        start = self._sanitize_trace_part(f"{float(segment.get('start', 0.0) or 0.0):.2f}s")
        end = self._sanitize_trace_part(f"{float(segment.get('end', 0.0) or 0.0):.2f}s")
        return f"segment_{segment_id}_{start}_{end}"

    def _copy_trace_image(self, segment, image_path):
        if not image_path:
            return ""
        try:
            source = Path(image_path)
            if not source.exists():
                return ""
            trace_session = self._ensure_model_input_trace_session()
            if trace_session is None:
                return ""
            suffix = source.suffix or ".jpg"
            target = trace_session / "images" / f"{self._build_trace_base_name(segment)}{suffix}"
            shutil.copy2(source, target)
            return str(target)
        except Exception as exc:
            print(f"Failed to copy model input trace image: {exc}")
            return ""

    def _build_trace_message_payload(self, segment, prompt, image_path, image_snapshot_path):
        return {
            "role": "user",
            "content": [
                {
                    "type": "image",
                    "source_path": str(image_path or ""),
                    "snapshot_path": str(image_snapshot_path or ""),
                    "file_name": Path(str(image_path)).name if image_path else "",
                },
                {
                    "type": "text",
                    "text_length": len(prompt or ""),
                    "text": prompt or "",
                },
            ],
            "segment": {
                "segment_id": segment.get("segment_id"),
                "start": segment.get("start"),
                "end": segment.get("end"),
                "duration": segment.get("duration"),
                "text_scope": segment.get("text_scope") or "window",
            },
        }

    def _segment_trace_context(self, segment):
        window_text = segment.get("window_text")
        if window_text is None:
            window_text = segment.get("text") or ""
        return {
            "segment_id": segment.get("segment_id"),
            "start": segment.get("start"),
            "end": segment.get("end"),
            "duration": segment.get("duration"),
            "text": segment.get("text") or "",
            "window_text": window_text or "",
            "source_transcript_text": segment.get("source_transcript_text") or "",
            "source_text_time_range": segment.get("source_text_time_range") or "",
            "text_scope": segment.get("text_scope") or "window",
            "source_type": segment.get("source_type"),
            "metadata_summary": segment.get("metadata_summary") or "",
            "sound_summary": segment.get("sound_summary") or "",
            "text_policy_summary": segment.get("text_policy_summary") or "",
            "text_policy_matches": segment.get("text_policy_matches") or [],
            "modalities": segment.get("modalities") or [],
            "long_text_context": bool(segment.get("long_text_context")),
            "has_risk_sound": bool(segment.get("has_risk_sound")),
            "risk_sounds": segment.get("risk_sounds") or [],
            "sound_events": segment.get("sound_events") or [],
            "best_frame_path": segment.get("best_frame_path") or "",
            "contact_sheet_path": segment.get("contact_sheet_path") or "",
            "segment_frames": [
                {
                    "frame_id": frame.get("frame_id"),
                    "timestamp": frame.get("timestamp"),
                    "path": frame.get("path"),
                    "frame_count": frame.get("frame_count"),
                    "reasons": frame.get("reasons"),
                }
                for frame in (segment.get("segment_frames") or [])
            ],
        }

    def _write_model_input_trace(
        self,
        segment,
        *,
        prompt="",
        chat_template_text="",
        message_payload=None,
        image_path="",
        image_snapshot_path="",
        generation_params=None,
        raw_model_response=None,
        parsed_result=None,
        final_audit_result=None,
        error=None,
    ):
        if not self.trace_model_inputs:
            return ""
        try:
            trace_session = self._ensure_model_input_trace_session()
            if trace_session is None:
                return ""
            trace_path = trace_session / f"{self._build_trace_base_name(segment)}.json"
            payload = {
                "trace_session_id": self._trace_session_id,
                "created_at": datetime.now().isoformat(timespec="seconds"),
                "prompt": prompt or "",
                "chat_template_text": chat_template_text or "",
                "message_payload": message_payload or {},
                "image_path": str(image_path or ""),
                "image_snapshot_path": str(image_snapshot_path or ""),
                "segment_context": self._segment_trace_context(segment),
                "generation_params": generation_params or {},
                "raw_model_response": raw_model_response,
                "parsed_result": parsed_result,
                "final_audit_result": final_audit_result,
                "error": error,
            }
            with open(trace_path, "w", encoding="utf-8") as trace_file:
                json.dump(self._json_safe(payload), trace_file, ensure_ascii=False, indent=2)
            trace_file_path = str(trace_path)
            if trace_file_path not in self._trace_files:
                self._trace_files.append(trace_file_path)
            segment["model_input_trace_file"] = trace_file_path
            return trace_file_path
        except Exception as exc:
            print(f"Failed to write model input trace: {exc}")
            return ""

    def update_model_input_trace_final_result(self, segment, final_audit_result):
        trace_file = (
            (segment.get("audit_result") or {}).get("model_input_trace_file")
            or segment.get("model_input_trace_file")
        )
        if not self.trace_model_inputs or not trace_file:
            return ""
        try:
            trace_path = Path(trace_file)
            payload = {}
            if trace_path.exists():
                with open(trace_path, "r", encoding="utf-8") as trace_handle:
                    payload = json.load(trace_handle)
            payload["final_audit_result"] = self._json_safe(final_audit_result)
            payload["updated_at"] = datetime.now().isoformat(timespec="seconds")
            with open(trace_path, "w", encoding="utf-8") as trace_handle:
                json.dump(self._json_safe(payload), trace_handle, ensure_ascii=False, indent=2)
            return str(trace_path)
        except Exception as exc:
            print(f"Failed to update model input trace final result: {exc}")
            return ""

    def _ensure_qwen_gpu_ready(self):
        if not self.qwen_target_device.startswith("cuda"):
            raise RuntimeError(
                f"Unsupported qwen target device '{self.qwen_target_device}'. "
                "This service requires Qwen/Qwen3-VL-4B-Instruct to run on GPU (cuda) and does not support CPU preloading."
            )
        if torch.version.cuda is None:
            raise RuntimeError(
                "Qwen/Qwen3-VL-4B-Instruct is configured for GPU, but the installed PyTorch build has no CUDA support. "
                "Use a CUDA-enabled PyTorch build on a GPU host."
            )
        if not torch.cuda.is_available():
            host_name = platform.system() or "Current"
            raise RuntimeError(
                "Qwen/Qwen3-VL-4B-Instruct is configured for GPU, but no CUDA device is available on this host. "
                f"{host_name} CPU-only hosts are not supported for full moderation service startup."
            )

    def _translate_qwen_load_error(self, exc):
        message = str(exc)
        lowered = message.lower()
        if "1455" in lowered or "页面文件太小" in message:
            return (
                f"Qwen/Qwen3-VL-4B-Instruct failed to load on target device {self.qwen_target_device}: "
                "Windows reported os error 1455 (page file too small / insufficient memory). "
                "This service does not support CPU preloading for Qwen. Use a CUDA-enabled GPU host."
            )
        if "out of memory" in lowered and "cuda" in lowered:
            return (
                f"Qwen/Qwen3-VL-4B-Instruct failed to load on target device {self.qwen_target_device}: "
                "CUDA out of memory. Use a GPU with more available VRAM or reduce competing GPU workloads."
            )
        if "cuda" in lowered and ("driver" in lowered or "available" in lowered or "initialization" in lowered):
            return (
                f"Qwen/Qwen3-VL-4B-Instruct failed to load on target device {self.qwen_target_device}: "
                f"{message}. Verify CUDA driver/runtime compatibility on the GPU host."
            )
        return (
            f"Qwen/Qwen3-VL-4B-Instruct failed to load on target device {self.qwen_target_device}: "
            f"{message}. Recommended environment: CPU for YAMNet/Whisper/VAD and a CUDA-enabled GPU host for Qwen."
        )

    def get_qwen_runtime_device(self):
        if self.qwen_runtime_device is not None:
            return self.qwen_runtime_device
        if self.qwen_model is None:
            return torch.device(self.qwen_target_device)

        hf_device_map = getattr(self.qwen_model, "hf_device_map", None)
        if isinstance(hf_device_map, dict):
            for mapped_device in hf_device_map.values():
                if mapped_device in (None, "cpu", "disk"):
                    continue
                if isinstance(mapped_device, int):
                    self.qwen_runtime_device = torch.device(f"cuda:{mapped_device}")
                    return self.qwen_runtime_device
                if isinstance(mapped_device, str):
                    self.qwen_runtime_device = torch.device(mapped_device)
                    return self.qwen_runtime_device

        try:
            self.qwen_runtime_device = next(self.qwen_model.parameters()).device
        except Exception:
            self.qwen_runtime_device = torch.device(self.qwen_target_device)
        return self.qwen_runtime_device
        
    def load_all_models(self):
        """加载所有模型"""
        print("开始加载所有模型...")
        
        # 加载YAMNet
        self._load_yamnet()
        
        # 加载Whisper
        self._load_whisper()
        
        # 加载QVEn3Guard
        self._load_qwen()

        # 加载VAD
        self._init_vad()
        
        print("所有模型加载完成！")
        return self
    
    def _load_yamnet(self):
        """加载YAMNet模型(CPU)"""
        try:
            import tensorflow as tf
            import tensorflow_hub as hub
            import csv
            
            print("正在加载YAMNet模型...")
            
            # YAMNet模型URL
            yamnet_url = "https://tfhub.dev/google/yamnet/1"
            yamnet_model_path = self.model_dir / "yamnet"
            
            # 下载或加载模型
            if not yamnet_model_path.exists():
                print(f"下载YAMNet模型到 {yamnet_model_path}")
                with tf.device("/CPU:0"):
                    self.yamnet_model = hub.load(yamnet_url)
                tf.saved_model.save(self.yamnet_model, str(yamnet_model_path))
            else:
                print(f"从本地加载YAMNet模型: {yamnet_model_path}")
                with tf.device("/CPU:0"):
                    self.yamnet_model = tf.saved_model.load(str(yamnet_model_path))
            
            # # 加载类别名称
            # class_map_path = self.yamnet_model.class_map_path().numpy()
            # class_names = []
            # with tf.io.gfile.GFile(class_map_path) as f:
            #     for line in f:
            #         class_names.append(line.strip().split(',')[2])
            # self.yamnet_class_names = class_names
            
            # # ✅ 移除或注释掉所有TFLite相关的代码
            # # 不需要转换，直接使用 self.yamnet_model 进行推理
            try:
                class_map_path = self.yamnet_model.class_map_path().numpy().decode('utf-8')
                
                # 下载类别文件（如果不存在）
                if not Path(class_map_path).exists():
                    import urllib.request
                    url = "https://storage.googleapis.com/audioset/yamnet_class_map.csv"
                    urllib.request.urlretrieve(url, class_map_path)
                
                # 读取CSV文件
                class_names = []
                with open(class_map_path, 'r') as f:
                    csv_reader = csv.reader(f)
                    next(csv_reader)  # 跳过标题行
                    for row in csv_reader:
                        if len(row) >= 3:
                            class_names.append(row[2])  # 第三列是display_name
            except:
                # 方法2：如果上面的方法失败，使用预定义的类别名称（部分）
                print("使用备用类别名称列表")
                class_names = [
                    "Speech", "Music", "Silence", "Gunshot", "Explosion",
                    "Scream", "Glass breaking", "Alarm", "Siren", "Fight",
                    "Crash", "Vehicle", "Engine", "Footsteps", "Door slam",
                    "Crying", "Laughter", "Cheering", "Applause", "Bark"
                ] + [f"Class_{i}" for i in range(500)]  # 补全到521类
            
            self.yamnet_class_names = class_names
            
            print(f"YAMNet模型加载完成，支持 {len(class_names)} 个类别")
            print(f"示例类别: {class_names[:5]}")
            
        except ImportError:
            print("TensorFlow未安装")
        except Exception as e:
            print(f"YAMNet加载失败: {e}")
            self.yamnet_model = None
        
    def _load_whisper(self):
        """加载Whisper模型 - CPU优化版本"""
        try:
            print("正在加载Whisper模型...")
            
            # 选择小型模型以获得更好的CPU性能
            model_size = "small"  # 可选: tiny, base, small, medium, large
            
            # 模型缓存路径
            cache_dir = self.model_dir / "whisper"
            cache_dir.mkdir(exist_ok=True)
            
            # 下载并加载模型
            self.whisper_model = whisper.load_model(
                model_size, 
                device='cpu',  # 强制使用CPU
                download_root=str(cache_dir)
            )
            
            print(f"Whisper {model_size} 模型加载完成")
            
        except Exception as e:
            print(f"Whisper加载失败: {e}")
            self.whisper_model = None

    def _load_qwen(self):
        """加载 Qwen3-VL-8B-Instruct 模型"""
        try:
            from transformers import Qwen3VLForConditionalGeneration, AutoProcessor
            import torch
            
            print("正在加载 Qwen3-VL-4B-Instruct 模型...")
            
            # 模型名称 - 使用4B版本
            model_name = "Qwen/Qwen3-VL-4B-Instruct"
            
            # 模型缓存路径
            cache_dir = self.model_dir / "qwen3-vl-4b"
            cache_dir.mkdir(exist_ok=True)
            
            # 加载processor
            self.qwen_processor = AutoProcessor.from_pretrained(
                model_name,
                cache_dir=cache_dir,
                trust_remote_code=True
            )

            # 直接加载模型
            self.qwen_model = Qwen3VLForConditionalGeneration.from_pretrained(
                model_name,
                cache_dir=cache_dir,
                trust_remote_code=True,
                torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32,
                device_map="auto" if torch.cuda.is_available() else None,
                low_cpu_mem_usage=True
            )

            if not torch.cuda.is_available():
                self.qwen_model.to(self.device)

            self.qwen_model.eval()
            
            print(f"✅ Qwen3-VL-4B-Instruct 模型加载完成")
            print(f"   运行设备: {self.qwen_model.device}")
                
        except ImportError as e:
            print(f"Qwen3-VL-4B 依赖库导入失败: {e}")
            self.qwen_model = None
            self.qwen_processor = None
        except Exception as e:
            print(f"Qwen3-VL-4B 加载失败: {e}")
            self.qwen_model = None
            self.qwen_processor = None
    
    def _init_vad(self):
        """初始化WebRTC VAD"""
        try:
            self.vad = webrtcvad.Vad(self.vad_mode)
            print(f"✅ VAD初始化成功，灵敏度模式: {self.vad_mode}")
        except Exception as e:
            print(f"❌ VAD初始化失败: {e}")
            self.vad = None
    # def _load_qwen(self):
    #     """加载Qwen2.5-VL-7B模型 - 多模态视觉语言模型"""
    #     try:
    #         from transformers import Qwen3VLForConditionalGeneration, AutoProcessor
    #         import torch
            
    #         print("正在加载Qwen2.5-VL-7B模型...")
            
    #         # 模型名称
    #         model_name = "Qwen/Qwen2.5-VL-7B-Instruct"  # 使用指令版本
            
    #         # 模型缓存路径
    #         cache_dir = self.model_dir / "qwen2.5-vl"
    #         cache_dir.mkdir(exist_ok=True)
            
    #         # 加载processor（包含tokenizer和图像处理器）
    #         self.qwen_processor = AutoProcessor.from_pretrained(
    #             model_name,
    #             cache_dir=cache_dir,
    #             trust_remote_code=True
    #         )
            
    #         # 加载模型 - 使用4-bit量化减少内存占用（可选）
    #         try:
    #             # 尝试使用bitsandbytes进行量化（如果安装了）
    #             import bitsandbytes as bnb
    #             self.qwen_model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
    #                 model_name,
    #                 cache_dir=cache_dir,
    #                 trust_remote_code=True,
    #                 torch_dtype=torch.float16,
    #                 device_map="auto",
    #                 load_in_4bit=True,  # 使用4-bit量化
    #                 bnb_4bit_compute_dtype=torch.float16
    #             )
    #             print("✅ 使用4-bit量化加载模型")
    #         except ImportError:
    #             # 如果没有bitsandbytes，使用标准加载
    #             self.qwen_model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
    #                 model_name,
    #                 cache_dir=cache_dir,
    #                 trust_remote_code=True,
    #                 torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32,
    #                 device_map="auto" if torch.cuda.is_available() else None,
    #                 low_cpu_mem_usage=True
    #             )
                
    #             # 如果没有GPU，将模型移到CPU
    #             if not torch.cuda.is_available():
    #                 self.qwen_model.to(self.device)
    #             print("✅ 使用标准方式加载模型")
            
    #         self.qwen_model.eval()
            
    #         print(f"✅ Qwen2.5-VL-7B模型加载完成")
    #         print(f"   模型参数量: 7B")
    #         print(f"   运行设备: {self.qwen_model.device}")
            
    #     except ImportError as e:
    #         print(f"Qwen2.5-VL-7B依赖库导入失败: {e}")
    #         print("请安装必要的包: pip install transformers torch accelerate bitsandbytes")
    #     except Exception as e:
    #         print(f"Qwen2.5-VL-7B加载失败: {e}")
            
    #         # # 如果7B模型太大，尝试使用更小的替代版本
    #         # try:
    #         #     print("尝试加载较小的Qwen2.5-VL-3B-Instruct版本...")
    #         #     model_name = "Qwen/Qwen2.5-VL-3B-Instruct"
                
    #         #     self.qwen_processor = AutoProcessor.from_pretrained(
    #         #         model_name,
    #         #         cache_dir=self.model_dir / "qwen2.5-vl-3b",
    #         #         trust_remote_code=True
    #         #     )
                
    #         #     self.qwen_model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
    #         #         model_name,
    #         #         cache_dir=self.model_dir / "qwen2.5-vl-3b",
    #         #         trust_remote_code=True,
    #         #         torch_dtype=torch.float32,
    #         #         low_cpu_mem_usage=True
    #         #     )
                
    #         #     if not torch.cuda.is_available():
    #         #         self.qwen_model.to(self.device)
                
    #         #     self.qwen_model.eval()
    #         #     print(f"✅ Qwen2.5-VL-3B-Instruct模型加载完成")
                
    #         # except Exception as e2:
    #         #     print(f"Qwen2.5-VL-3B加载也失败: {e2}")
    #         #     self.qwen_model = None
    #         #     self.qwen_processor = None
    
    
    def get_model_info(self):
        """获取模型信息"""
        info = {
            "device": str(self.device),
            "yamnet": {
                "loaded": self.yamnet_model is not None or self.yamnet_interpreter is not None,
                "classes": len(self.yamnet_class_names) if self.yamnet_class_names else 0
            },
            "whisper": {
                "loaded": self.whisper_model is not None,
                "model_type": "whisper" if self.whisper_model else None
            },
            "qwen_vl": {
                "loaded": self.qwen_model is not None,
                "model_type": "Qwen3-VL-8B-Instruct" if self.qwen_model else None,
                "device": str(self.qwen_model.device) if self.qwen_model else None
            }
        }
        return info

    def video_has_audio_stream(self, video_path):
        """Return True when ffprobe finds at least one audio stream."""
        probe = ffmpeg.probe(video_path)
        return any(stream.get("codec_type") == "audio" for stream in probe.get("streams", []))

    def get_video_duration(self, video_path):
        """Best-effort video duration in seconds."""
        try:
            probe = ffmpeg.probe(video_path)
            if probe.get("format", {}).get("duration"):
                return float(probe["format"]["duration"])
            for stream in probe.get("streams", []):
                if stream.get("codec_type") == "video" and stream.get("duration"):
                    return float(stream["duration"])
        except Exception as exc:
            print(f"Failed to probe video duration: {exc}")
        return 0.0

    def extract_audio_from_video(self, video_path, output_audio_path=None, sample_rate=16000):
        """
        从视频文件中提取音频
        
        Args:
            video_path: 视频文件路径
            output_audio_path: 输出音频文件路径（可选，如果不提供则创建临时文件）
            sample_rate: 音频采样率（Whisper推荐16000Hz）
            
        Returns:
            output_audio_path: 提取的音频文件路径
        """
        try:
            # 如果没有指定输出路径，创建临时文件
            if output_audio_path is None: 
                temp_dir = Path("./temp")
                temp_dir.mkdir(exist_ok=True)
                output_audio_path = str(temp_dir / f"audio_{uuid.uuid4().hex[:8]}.wav")
                print(f"音频保存到: {output_audio_path}")
            
            print(f"从视频提取音频: {video_path} -> {output_audio_path}")
            
            # 使用 ffmpeg-python 提取音频
            (
                ffmpeg
                .input(video_path)
                .output(
                    output_audio_path,
                    acodec='pcm_s16le',  # 16-bit PCM编码
                    ac=1,                 # 单声道
                    ar=sample_rate        # 采样率
                )
                .overwrite_output()
                .run(capture_stdout=True, capture_stderr=True)
            )
            
            print(f"音频提取成功: {output_audio_path}")
            return output_audio_path
            
        except ffmpeg.Error as e:
            print(f"FFmpeg错误: {e.stderr.decode()}")
            raise
        except Exception as e:
            print(f"音频提取失败: {e}")
            raise

    def get_audio_duration(self, audio_path):
        """
        获取音频文件时长（秒）
        
        Args:
            audio_path: 音频文件路径
            
        Returns:
            float: 音频时长（秒）
        """
        try:
            import ffmpeg
            
            # 使用 ffmpeg 探测音频文件信息
            probe = ffmpeg.probe(audio_path)
            audio_info = next(s for s in probe['streams'] if s['codec_type'] == 'audio')
            duration = float(audio_info['duration'])
            return duration
            
        except Exception as e:
            print(f"获取音频时长失败: {e}")
            return 0
        
    def detect_voice_segments(self, audio_path, min_segment_duration=5, max_silence_duration=2):
        """
        检测音频中的语音段落
        
        Args:
            audio_path: 音频文件路径
            min_segment_duration: 最小语音段落时长（秒）
            max_silence_duration: 最大静音容忍时长（秒）
            
        Returns:
            list: 语音段落列表 [{"start": float, "end": float, "duration": float}, ...]
        """
        if self.vad is None:
            print("❌ VAD未初始化")
            return []
        
        print(f"开始VAD检测: {audio_path}")
        
        try:
            # 1. 读取音频文件
            import wave
            import contextlib
            
            # 使用wave模块读取音频文件
            with contextlib.closing(wave.open(audio_path, 'rb')) as wf:
                num_channels = wf.getnchannels()
                sample_width = wf.getsampwidth()
                sample_rate = wf.getframerate()
                num_frames = wf.getnframes()
                
                print(f"音频信息: {sample_rate}Hz, {num_channels}通道, {sample_width*8}bit")
                
                # 读取所有音频数据
                audio_data = wf.readframes(num_frames)
                
            # 2. 确保音频格式符合VAD要求 (16-bit PCM, 单声道, 16000Hz)
            # 如果不是16000Hz，需要重采样（这里简化处理，假设已经是16000Hz）
            if sample_rate != 16000:
                print(f"警告: VAD需要16000Hz采样率，当前是{sample_rate}Hz，可能影响检测效果")
            
            # 如果是立体声，转换为单声道（取平均值）
            if num_channels == 2:
                print("将立体声转换为单声道")
                # 将16-bit立体声数据转换为单声道
                audio_array = np.frombuffer(audio_data, dtype=np.int16)
                audio_array = audio_array.reshape(-1, 2).mean(axis=1).astype(np.int16)
                audio_data = audio_array.tobytes()
            
            # 3. 逐帧检测
            frame_duration_ms = 30  # VAD帧时长（毫秒）
            frame_size = int(sample_rate * frame_duration_ms / 1000)  # 每帧样本数
            num_frames = len(audio_data) // (frame_size * 2)  # 2 bytes per sample for 16-bit
            
            # 存储每帧的检测结果
            is_speech = []
            timestamps = []
            
            for i in range(num_frames):
                start = i * frame_size * 2
                end = start + frame_size * 2
                frame = audio_data[start:end]
                
                # 确保帧长度正确
                if len(frame) < frame_size * 2:
                    # 最后一帧可能不完整，补零
                    frame = frame.ljust(frame_size * 2, b'\x00')
                
                # VAD检测
                try:
                    is_speech_frame = self.vad.is_speech(frame, sample_rate)
                    is_speech.append(is_speech_frame)
                    timestamps.append(i * frame_duration_ms / 1000.0)  # 转换为秒
                except Exception as e:
                    print(f"帧 {i} 检测失败: {e}")
                    is_speech.append(False)
                    timestamps.append(i * frame_duration_ms / 1000.0)
            
            # 4. 合并连续的语音帧
            voice_segments = []
            in_speech = False
            segment_start = 0
            
            for i, speech in enumerate(is_speech):
                current_time = timestamps[i]
                
                if speech and not in_speech:
                    # 语音开始
                    in_speech = True
                    segment_start = current_time
                elif not speech and in_speech:
                    # 语音结束
                    in_speech = False
                    segment_end = current_time
                    
                    # 检查段落时长是否满足最小要求
                    if segment_end - segment_start >= min_segment_duration:
                        voice_segments.append({
                            "start": segment_start,
                            "end": segment_end,
                            "duration": segment_end - segment_start
                        })
            
            # 处理最后可能还在语音中的情况
            if in_speech:
                segment_end = timestamps[-1] + frame_duration_ms / 1000.0
                if segment_end - segment_start >= min_segment_duration:
                    voice_segments.append({
                        "start": segment_start,
                        "end": segment_end,
                        "duration": segment_end - segment_start
                    })
            
            # 5. 合并相邻的语音段落（如果间隔小于max_silence_duration）
            if len(voice_segments) > 1:
                merged_segments = []
                current = voice_segments[0]
                
                for next_seg in voice_segments[1:]:
                    if next_seg["start"] - current["end"] <= max_silence_duration:
                        # 合并
                        current = {
                            "start": current["start"],
                            "end": next_seg["end"],
                            "duration": next_seg["end"] - current["start"]
                        }
                    else:
                        merged_segments.append(current)
                        current = next_seg
                
                merged_segments.append(current)
                voice_segments = merged_segments
            
            print(f"✅ VAD检测完成，找到 {len(voice_segments)} 个语音段落")
            return voice_segments
            
        except Exception as e:
            print(f"❌ VAD检测失败: {e}")
            import traceback
            traceback.print_exc()
            return []

    def print_voice_segments(self, segments, audio_duration=None):
        """打印语音段落信息"""
        if not segments:
            print("没有检测到语音段落")
            return
        
        print("\n📢 语音段落检测结果:")
        print("-" * 50)
        total_speech_time = 0
        
        for i, seg in enumerate(segments):
            print(f"段落 {i+1}: {seg['start']:.2f}s → {seg['end']:.2f}s (时长: {seg['duration']:.2f}s)")
            total_speech_time += seg['duration']
        
        if audio_duration:
            print("-" * 50)
            print(f"总音频时长: {audio_duration:.2f}s")
            print(f"总语音时长: {total_speech_time:.2f}s")
            print(f"语音占比: {total_speech_time/audio_duration*100:.1f}%")
        print("-" * 50)

    def _merge_sound_events(self, events, time_window=1.0):
        """
        合并相近时间的声音事件
        
        Args:
            events: 原始事件列表
            time_window: 时间窗口（秒），同一窗口内的同类事件合并
            
        Returns:
            list: 合并后的事件列表
        """
        if not events:
            return []
        
        # 按时间排序
        events.sort(key=lambda x: x['time'])
        
        merged = []
        current = events[0]
        
        for next_event in events[1:]:
            # 如果是同一类别且时间相近，合并
            if (next_event['class'] == current['class'] and 
                next_event['time'] - current['time'] <= time_window):
                # 保留置信度较高的
                if next_event['confidence'] > current['confidence']:
                    current['confidence'] = next_event['confidence']
                # 时间取平均值
                current['time'] = (current['time'] + next_event['time']) / 2
            else:
                merged.append(current)
                current = next_event
        
        merged.append(current)
        return merged

    def detect_sound_events(self, audio_path, top_k=5, confidence_threshold=0.3):
        """
        使用YAMNet检测音频中的声音事件
        
        Args:
            audio_path: 音频文件路径
            top_k: 每帧返回的前k个最可能的声音类别
            confidence_threshold: 置信度阈值
            
        Returns:
            list: 声音事件列表，每个事件包含时间戳、类别和置信度
        """
        if self.yamnet_model is None:
            print("❌ YAMNet模型未加载")
            return []
        
        print(f"开始YAMNet声学检测: {audio_path}")
        
        try:
            # 1. 读取音频文件
            import wave
            import contextlib
            
            with contextlib.closing(wave.open(audio_path, 'rb')) as wf:
                num_channels = wf.getnchannels()
                sample_width = wf.getsampwidth()
                sample_rate = wf.getframerate()
                num_frames = wf.getnframes()
                
                print(f"音频信息: {sample_rate}Hz, {num_channels}通道, {sample_width*8}bit")
                
                # 读取所有音频数据
                audio_data = wf.readframes(num_frames)
                
            # 2. 转换为numpy数组
            audio_array = np.frombuffer(audio_data, dtype=np.int16).astype(np.float32) / 32768.0
            
            # 3. 如果是立体声，转换为单声道
            if num_channels == 2:
                print("将立体声转换为单声道")
                audio_array = audio_array.reshape(-1, 2).mean(axis=1)
            
            # 4. 重采样到16000Hz（如果需要）
            if sample_rate != self.yamnet_sample_rate:
                print(f"重采样从 {sample_rate}Hz 到 {self.yamnet_sample_rate}Hz")
                # 使用scipy进行重采样（需要安装scipy）
                try:
                    from scipy import signal
                    number_of_samples = int(len(audio_array) * self.yamnet_sample_rate / sample_rate)
                    audio_array = signal.resample(audio_array, number_of_samples)
                except ImportError:
                    print("⚠️ scipy未安装，跳过重采样，可能影响检测效果")
            
            # 5. YAMNet推理
            # YAMNet期望输入形状为 [batch, samples]
            scores, embeddings, spectrogram = self.yamnet_model(audio_array)
            
            # 6. 解析结果
            sound_events = []
            
            # 定义需要关注的风险声音类别（可以根据需要调整）
            risk_sound_categories = {
                'gunshot': ['Gunshot', 'gunshot', 'firearm'],
                'explosion': ['Explosion', 'explosion', 'blast'],
                'scream': ['Scream', 'screaming', 'shriek', 'crying', 'sobbing'],
                'glass_break': ['Glass', 'breaking', 'smash'],
                'alarm': ['Alarm', 'siren', 'alert'],
                'fight': ['Fight', 'struggle', 'crash'],
                'sexual_moan': ['Moan', 'groan', 'pant', 'breathing', 'gasp'],
                'distress': ['Whimper', 'wail', 'crying', 'sob', 'screaming', 'yell'],
                'riot': ['Crowd', 'shout', 'chant', 'riot', 'booing'],
                'police_siren': ['Police car', 'siren', 'emergency vehicle']
            }
            
            # 将scores转换为numpy数组
            scores_np = scores.numpy()
            
            # 获取每个时间帧的top_k预测
            for frame_idx, frame_scores in enumerate(scores_np):
                # 获取该帧的top_k类别索引
                top_indices = np.argsort(frame_scores)[-top_k:][::-1]
                
                frame_time = frame_idx * self.yamnet_frame_hop_seconds
                
                for idx in top_indices:
                    confidence = frame_scores[idx]
                    class_name = self.yamnet_class_names[idx]
                    
                    if confidence >= confidence_threshold:
                        # 检查是否属于风险类别
                        is_risk = False
                        risk_category = None
                        
                        for category, keywords in risk_sound_categories.items():
                            if any(keyword.lower() in class_name.lower() for keyword in keywords):
                                is_risk = True
                                risk_category = category
                                break
                        
                        # 记录所有高置信度的声音，但标记风险类别
                        sound_events.append({
                            "time": round(frame_time, 2),
                            "class": class_name,
                            "confidence": float(confidence),
                            "is_risk": is_risk,
                            "risk_category": risk_category if is_risk else None
                        })
            
            # 7. 去重并合并相近时间的事件
            merged_events = self._merge_sound_events(sound_events)
            
            print(f"✅ YAMNet检测完成，找到 {len(merged_events)} 个声音事件")
            
            # 8. 统计风险事件
            risk_events = [e for e in merged_events if e['is_risk']]
            if risk_events:
                print(f"⚠️ 检测到 {len(risk_events)} 个风险声音事件")
                for event in risk_events[:5]:  # 只显示前5个
                    print(f"   {event['time']}s: {event['class']} ({event['confidence']:.2f}) [{event['risk_category']}]")
            
            return merged_events
            
        except Exception as e:
            print(f"❌ YAMNet检测失败: {e}")
            import traceback
            traceback.print_exc()
            return []
        
    def print_sound_events(self, events, top_n=20):
        """
        打印声音事件检测结果
        
        Args:
            events: 声音事件列表
            top_n: 显示前N个事件
        """
        if not events:
            print("没有检测到声音事件")
            return
        
        print("\n🎵 YAMNet声音事件检测结果:")
        print("-" * 70)
        
        # 统计各类别出现次数
        class_counts = {}
        risk_counts = {}
        
        for event in events:
            class_name = event['class']
            class_counts[class_name] = class_counts.get(class_name, 0) + 1
            
            if event['is_risk']:
                risk_counts[event['risk_category']] = risk_counts.get(event['risk_category'], 0) + 1
        
        # 显示前top_n个事件
        print(f"前 {min(top_n, len(events))} 个事件详情:")
        for i, event in enumerate(events[:top_n]):
            risk_marker = "⚠️" if event['is_risk'] else "  "
            print(f"  {risk_marker} {event['time']:6.2f}s | {event['class']:30} | 置信度: {event['confidence']:.3f}")
        
        # 显示统计信息
        print("-" * 70)
        print(f"总事件数: {len(events)}")
        
        if class_counts:
            print("\n主要声音类别:")
            sorted_classes = sorted(class_counts.items(), key=lambda x: x[1], reverse=True)[:5]
            for class_name, count in sorted_classes:
                print(f"  {class_name}: {count}次")
        
        if risk_counts:
            print("\n⚠️ 风险声音统计:")
            for category, count in risk_counts.items():
                print(f"  {category}: {count}次")
        
        print("-" * 70)

    def transcribe_audio_segments(self, audio_path, voice_segments, language='zh'):
        """
        对VAD检测出的语音段落进行Whisper转写
        
        Args:
            audio_path: 音频文件路径
            voice_segments: VAD检测出的语音段落列表
            language: 语言代码（'zh'中文，'en'英文，None自动检测）
            
        Returns:
            list: 转写结果列表，每个元素包含时间段和文本
        """
        if self.whisper_model is None:
            print("❌ Whisper模型未加载")
            return []
        
        print(f"开始Whisper语音转写，共 {len(voice_segments)} 个语音段落")
        
        try:
            import whisper
            import numpy as np
            
            # 加载音频
            audio = whisper.load_audio(audio_path)
            
            transcriptions = []
            
            for i, segment in enumerate(voice_segments):
                start_time = segment['start']
                end_time = segment['end']
                
                print(f"转写段落 {i+1}/{len(voice_segments)}: {start_time:.2f}s - {end_time:.2f}s")
                
                # 提取对应时间段的音频
                start_sample = int(start_time * 16000)
                end_sample = int(end_time * 16000)
                segment_audio = audio[start_sample:end_sample]
                
                # 忽略过短的音频段
                if len(segment_audio) < 1600:  # 小于0.1秒
                    print(f"  段落太短，跳过")
                    continue
                
                # Whisper转写
                options = {
                    "language": language,
                    "task": "transcribe",
                    "fp16": False,  # CPU模式
                    "word_timestamps": True
                }
                
                result = self.whisper_model.transcribe(segment_audio, **options)
                
                text = result["text"].strip()
                
                if text:  # 只保留有内容的转写
                    whisper_segments, words = self._normalize_whisper_segments(
                        result.get("segments") or [],
                        start_time,
                    )
                    transcriptions.append({
                        "segment_id": i + 1,
                        "start": round(start_time, 2),
                        "end": round(end_time, 2),
                        "duration": round(end_time - start_time, 2),
                        "text": text,
                        "language": result.get("language", language),
                        "segments": whisper_segments,
                        "words": words,
                    })
                    print(f"  ✅ 转写完成: {text[:50]}...")
                else:
                    print(f"  ⚠️ 转写结果为空")
            
            print(f"✅ Whisper转写完成，成功转写 {len(transcriptions)}/{len(voice_segments)} 个段落")
            return transcriptions
            
        except Exception as e:
            print(f"❌ Whisper转写失败: {e}")
            import traceback
            traceback.print_exc()
            return []
        
    def print_transcriptions(self, transcriptions):
        """
        打印转写结果
        
        Args:
            transcriptions: 转写结果列表
        """
        if not transcriptions:
            print("没有转写结果")
            return
        
        print("\n📝 Whisper转写结果:")
        print("=" * 80)
        
        total_duration = 0
        for i, item in enumerate(transcriptions):
            print(f"[段落 {item['segment_id']}] {item['start']:.2f}s - {item['end']:.2f}s (时长: {item['duration']:.2f}s)")
            print(f"文本: {item['text']}")
            print(f"语言: {item['language']}")
            print("-" * 40)
            total_duration += item['duration']
        
        print(f"总计: {len(transcriptions)} 个段落，总时长: {total_duration:.2f}s")
        print("=" * 80)
    
    @staticmethod
    def _add_frame_index_for_time(target_indices, timestamp, fps, total_frames):
        if timestamp is None or fps <= 0 or total_frames <= 0:
            return
        clamped = max(0.0, min(float(timestamp), (total_frames - 1) / fps))
        target_indices.add(int(round(clamped * fps)))

    def extract_keyframes(self, video_path, extraction_rate=1, focus_times=None):
        """
        从视频中提取关键帧
        
        Args:
            video_path: 视频文件路径
            extraction_rate: 每秒提取帧数
            
        Returns:
            list: 帧信息列表，包含时间戳和文件路径
        """
        print(f"开始提取视频关键帧: {video_path}")
        
        try:
            # 创建临时目录存放帧
            session_id = uuid.uuid4().hex[:8]
            frames_dir = self.keyframes_dir / session_id
            frames_dir.mkdir(exist_ok=True)
            
            # 打开视频文件
            cap = cv2.VideoCapture(video_path)
            fps = cap.get(cv2.CAP_PROP_FPS)
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            if not cap.isOpened() or fps <= 0 or total_frames <= 0:
                cap.release()
                print("Video has invalid fps or frame count; no keyframes extracted.")
                return []
            duration = total_frames / fps
            
            print(f"视频信息: {fps}fps, 总帧数:{total_frames}, 时长:{duration:.2f}s")
            
            # 计算提取间隔
            frame_interval = int(fps / extraction_rate)
            if frame_interval < 1:
                frame_interval = 1
            required_frame_indices = set()
            for timestamp in [0.0, 1.0, 2.0, duration - 2.0, duration - 1.0, duration - (1.0 / fps)]:
                self._add_frame_index_for_time(required_frame_indices, timestamp, fps, total_frames)
            for timestamp in focus_times or []:
                for offset in (-0.5, 0.0, 0.5):
                    self._add_frame_index_for_time(required_frame_indices, float(timestamp) + offset, fps, total_frames)
            
            frames_info = []
            frame_count = 0
            extracted_count = 0
            saved_frame_counts = set()
            previous_frame = None
            previous_small = None
            previous_frame_count = None
            capture_next_transition = False
            transition_threshold = 35.0

            def save_frame(frame_to_save, source_frame_count, reason):
                nonlocal extracted_count
                if frame_to_save is None or source_frame_count in saved_frame_counts:
                    return
                timestamp_to_save = source_frame_count / fps
                frame_path = frames_dir / f"frame_{extracted_count:04d}_{timestamp_to_save:.2f}s.jpg"
                cv2.imwrite(str(frame_path), frame_to_save)
                frames_info.append({
                    "frame_id": extracted_count,
                    "timestamp": round(timestamp_to_save, 2),
                    "path": str(frame_path),
                    "frame_count": source_frame_count,
                    "reasons": [reason],
                })
                saved_frame_counts.add(source_frame_count)
                extracted_count += 1
            
            while True:
                ret, frame = cap.read()
                if not ret:
                    break
                
                # 按间隔提取帧
                if frame_count % frame_interval == 0 or frame_count in required_frame_indices:
                    timestamp = frame_count / fps
                    
                    # 保存帧
                    frame_path = frames_dir / f"frame_{extracted_count:04d}_{timestamp:.2f}s.jpg"
                    cv2.imwrite(str(frame_path), frame)
                    
                    frames_info.append({
                        "frame_id": extracted_count,
                        "timestamp": round(timestamp, 2),
                        "path": str(frame_path),
                        "frame_count": frame_count
                    })
                    saved_frame_counts.add(frame_count)
                    extracted_count += 1
                
                small = cv2.resize(frame, (96, 54))
                small = cv2.cvtColor(small, cv2.COLOR_BGR2GRAY)
                if previous_small is not None:
                    diff = float(np.mean(cv2.absdiff(previous_small, small)))
                    if diff >= transition_threshold:
                        save_frame(previous_frame, previous_frame_count, "transition_before")
                        save_frame(frame, frame_count, "transition_after")
                        capture_next_transition = True
                    elif capture_next_transition:
                        save_frame(frame, frame_count, "transition_followup")
                        capture_next_transition = False

                previous_frame = frame.copy()
                previous_small = small
                previous_frame_count = frame_count

                frame_count += 1
            
            cap.release()
            frames_info.sort(key=lambda item: (item.get("timestamp", 0.0), item.get("frame_count", 0)))
            for frame_id, frame_info in enumerate(frames_info):
                frame_info["frame_id"] = frame_id
            print(f"✅ 关键帧提取完成，共提取 {extracted_count} 帧，保存到: {frames_dir}")
            
            return frames_info
            
        except Exception as e:
            print(f"❌ 关键帧提取失败: {e}")
            import traceback
            traceback.print_exc()
            return []
        
    @staticmethod
    def _read_meta_value(meta, *names):
        if meta is None:
            return None
        if hasattr(meta, "model_dump"):
            meta = meta.model_dump()
        for name in names:
            if isinstance(meta, dict) and meta.get(name) not in (None, ""):
                return meta.get(name)
            if hasattr(meta, name):
                value = getattr(meta, name)
                if value not in (None, ""):
                    return value
        return None

    def _build_metadata_summary(self, video_meta=None, item_meta=None):
        fields = [
            ("title", self._read_meta_value(video_meta, "video_name", "videoName", "title")),
            ("introduction", self._read_meta_value(video_meta, "introduction", "description")),
            ("tags", self._read_meta_value(video_meta, "tags")),
            ("cover", self._read_meta_value(video_meta, "video_cover", "videoCover")),
            ("file_name", self._read_meta_value(item_meta, "file_name", "fileName")),
            ("file_path", self._read_meta_value(item_meta, "file_path", "filePath")),
        ]
        parts = [f"{name}={value}" for name, value in fields if value not in (None, "")]
        return "; ".join(parts)

    @staticmethod
    def _clip_score(value, default=0.0):
        try:
            score = float(value)
        except Exception:
            score = default
        return max(0.0, min(1.0, score))

    @staticmethod
    def _compact_text(value):
        return str(value or "").strip()

    @staticmethod
    def _truncate_text(value, limit=1200):
        text = str(value or "").strip()
        if len(text) <= limit:
            return text
        return f"{text[:limit]}...[truncated {len(text) - limit} chars]"

    @staticmethod
    def _contains_any(text, keywords):
        lowered = str(text or "").lower()
        return any(str(keyword).lower() in lowered for keyword in keywords)

    @staticmethod
    def _severity_rank(severity):
        ranks = {"low": 1, "medium": 2, "high": 3, "critical": 4}
        return ranks.get(str(severity or "").lower(), 0)

    @staticmethod
    def _policy_action_rank(action):
        ranks = {"pass": 0, "monitor": 1, "review": 2, "reject": 3}
        return ranks.get(str(action or "").lower(), 0)

    @classmethod
    def _policy_score_floor(cls, match):
        action = str(match.get("policy_action") or "").lower()
        severity = str(match.get("severity") or "").lower()
        if action == "reject" or severity in {"high", "critical"}:
            return 0.82
        if action == "review" or severity == "medium":
            return 0.5
        if action == "monitor" or severity == "low":
            return 0.32
        return 0.0

    @classmethod
    def _scan_policy_text(cls, text, source):
        matches = []
        haystack = str(text or "")
        if not haystack.strip():
            return matches
        lowered = haystack.lower()
        for rule in cls.TEXT_POLICY_RULES:
            for keyword in rule["keywords"]:
                if str(keyword).lower() in lowered:
                    matches.append({
                        "risk_type": rule["risk_type"],
                        "risk_subtype": rule["risk_subtype"],
                        "severity": rule["severity"],
                        "policy_action": rule["policy_action"],
                        "keyword": keyword,
                        "source": source,
                    })
                    break
        return matches

    @classmethod
    def _select_strongest_policy_match(cls, matches):
        if not matches:
            return None
        return max(
            matches,
            key=lambda item: (
                cls._policy_action_rank(item.get("policy_action")),
                cls._severity_rank(item.get("severity")),
                cls._policy_score_floor(item),
            ),
        )

    @classmethod
    def _format_text_policy_summary(cls, matches):
        if not matches:
            return "无文本规则命中"
        parts = []
        for match in matches[:5]:
            parts.append(
                "{risk_type}/{risk_subtype}/{policy_action}/{severity}: keyword={keyword}, source={source}".format(
                    risk_type=match.get("risk_type"),
                    risk_subtype=match.get("risk_subtype"),
                    policy_action=match.get("policy_action"),
                    severity=match.get("severity"),
                    keyword=match.get("keyword"),
                    source=match.get("source"),
                )
            )
        return "; ".join(parts)

    @classmethod
    def _scan_segment_text_policy(cls, text, metadata_summary):
        matches = []
        matches.extend(cls._scan_policy_text(text, "speech_text"))
        matches.extend(cls._scan_policy_text(metadata_summary, "metadata_text"))
        deduped = []
        seen = set()
        for match in matches:
            key = (
                match.get("risk_type"),
                match.get("risk_subtype"),
                match.get("keyword"),
                match.get("source"),
            )
            if key not in seen:
                deduped.append(match)
                seen.add(key)
        return deduped

    @classmethod
    def _infer_content_context(cls, segment=None, result=None):
        result = result or {}
        explicit = str(result.get("content_context") or "").strip().lower()
        if explicit in {"game", "gaming", "film", "movie", "drama", "animation", "anime", "fictional", "news", "real"}:
            if explicit in {"gaming"}:
                return "game"
            if explicit in {"movie", "drama"}:
                return "film"
            if explicit in {"anime"}:
                return "animation"
            return explicit

        segment = segment or {}
        combined = " ".join([
            str(segment.get("text") or ""),
            str(segment.get("metadata_summary") or ""),
            str(result.get("reason") or ""),
        ])
        lowered = combined.lower()
        if any(keyword.lower() in lowered for keyword in ["游戏", "玩家", "第一人称", "手游", "端游", "fps", "game", "gameplay", "hud"]):
            return "game"
        if any(keyword.lower() in lowered for keyword in ["电影", "影视", "剪辑", "剧集", "电视剧", "movie", "film", "drama"]):
            return "film"
        if any(keyword.lower() in lowered for keyword in ["动漫", "动画", "漫画", "二次元", "anime", "animation"]):
            return "animation"
        if any(keyword.lower() in lowered for keyword in ["新闻", "报道", "纪录片", "news", "documentary"]):
            return "news"
        return explicit or "unknown"

    @classmethod
    def _is_fictional_context(cls, context):
        return str(context or "").lower() in {"game", "film", "animation", "fictional"}

    @classmethod
    def _has_hard_violence_evidence(cls, segment=None, result=None):
        segment = segment or {}
        result = result or {}
        combined = " ".join([
            str(segment.get("text") or ""),
            str(segment.get("metadata_summary") or ""),
            str(result.get("reason") or ""),
            str(result.get("risk_subtype") or ""),
        ])
        for negative_phrase in [
            "无血腥", "没有血腥", "无流血", "没有流血", "无真实伤害", "没有真实伤害",
            "非真实暴力", "无教程", "没有教程", "无威胁", "没有威胁",
            "no blood", "without blood", "no gore", "no real harm", "not real violence",
            "no tutorial", "without tutorial", "no threat",
        ]:
            combined = combined.replace(negative_phrase, "")
        return cls._contains_any(combined, cls.HARD_VIOLENCE_KEYWORDS)

    @staticmethod
    def _normalize_evidence_modalities(value):
        if value is None:
            return []
        if isinstance(value, str):
            raw_items = re.split(r"[,，/、\s]+", value)
            return [item.strip() for item in raw_items if item.strip()]
        if isinstance(value, (list, tuple, set)):
            return [str(item).strip() for item in value if str(item).strip()]
        return [str(value).strip()]

    @classmethod
    def _segment_evidence_modalities(cls, segment, result):
        modalities = set(cls._normalize_evidence_modalities(result.get("evidence_modalities")))
        modalities.update(str(item) for item in segment.get("modalities") or [] if item)
        if segment.get("has_risk_sound"):
            modalities.add("audio_event")
        if segment.get("text_policy_matches"):
            modalities.add("text_policy")
        return sorted(modalities)

    @classmethod
    def _apply_text_policy_fusion(cls, segment, result):
        result = cls._normalize_audit_result(result, segment)
        matches = segment.get("text_policy_matches") or []
        if not matches:
            return result
        strongest = cls._select_strongest_policy_match(matches)
        if not strongest:
            return result

        current_type = cls.normalize_risk_type(result.get("risk_type"))
        current_score = cls._clip_score(result.get("risk_score"))
        policy_floor = cls._policy_score_floor(strongest)
        strongest_action = str(strongest.get("policy_action") or "").lower()
        if (
            strongest_action == "reject"
            or current_type in {"normal", "other", "audio_event"}
            or policy_floor > current_score
        ):
            result["risk_type"] = strongest["risk_type"]
            result["risk_subtype"] = strongest["risk_subtype"]
        result["risk_score"] = max(current_score, policy_floor)
        result["severity"] = strongest.get("severity")
        result["policy_action"] = strongest.get("policy_action")
        result["is_risky"] = result["risk_score"] > 0.3
        result["text_policy_matches"] = matches
        result["evidence_modalities"] = cls._segment_evidence_modalities(segment, result)

        reason = str(result.get("reason") or "").strip()
        policy_reason = f"Text policy precheck hit: {cls._format_text_policy_summary(matches)}."
        if policy_reason not in reason:
            result["reason"] = f"{reason} {policy_reason}".strip()
        return result

    @classmethod
    def _apply_business_calibration(cls, segment, result):
        result = cls._normalize_audit_result(result, segment)
        if result.get("audit_incomplete"):
            return result

        risk_type = cls.normalize_risk_type(result.get("risk_type"))
        score = cls._clip_score(result.get("risk_score"))
        context = cls._infer_content_context(segment, result)
        action = str(result.get("policy_action") or "").lower()
        evidence_modalities = cls._segment_evidence_modalities(segment, result)
        result["content_context"] = context
        result["evidence_modalities"] = evidence_modalities

        if risk_type == "violence" and cls._is_fictional_context(context) and not cls._has_hard_violence_evidence(segment, result):
            if score > 0.65:
                score = 0.65
            result["policy_action"] = "review"
            result["severity"] = result.get("severity") or "medium"
            reason = str(result.get("reason") or "")
            note = "Fictional/game/film violence without hard harm evidence is capped to manual review."
            if note not in reason:
                result["reason"] = f"{reason} {note}".strip()

        elif action == "reject" or risk_type in cls.ZERO_TOLERANCE_RISK_TYPES:
            if score > 0.3:
                score = max(score, 0.78)
                result["policy_action"] = "reject"
                result["severity"] = result.get("severity") or "high"

        elif action == "review":
            score = max(score, 0.5)
            if risk_type not in cls.ZERO_TOLERANCE_RISK_TYPES:
                score = min(score, 0.7)

        if risk_type == "normal" and score > 0.3:
            risk_type = "other"
            result["risk_type"] = risk_type

        result["risk_score"] = cls._clip_score(score)
        result["is_risky"] = result["risk_score"] > 0.3 or risk_type not in {"normal", "none"}
        return cls._normalize_audit_result(result, segment)

    @classmethod
    def _is_direct_reject_segment(cls, segment):
        result = segment.get("audit_result") or {}
        risk_type = cls.normalize_risk_type(result.get("risk_type"))
        score = cls._clip_score(result.get("risk_score"))
        action = str(result.get("policy_action") or "").lower()
        if result.get("audit_incomplete"):
            return False
        if action == "reject" and score > 0.7:
            return True
        if risk_type in cls.ZERO_TOLERANCE_RISK_TYPES and score > 0.7:
            return True
        if risk_type == "violence" and score > 0.7:
            context = cls._infer_content_context(segment, result)
            return not cls._is_fictional_context(context) or cls._has_hard_violence_evidence(segment, result)
        return False

    @classmethod
    def _decide_report_level(cls, aligned_segments, audit_complete):
        total_duration = sum(float(seg.get("duration", 0.0) or 0.0) for seg in aligned_segments)
        high_segments = [
            seg for seg in aligned_segments
            if cls._clip_score((seg.get("audit_result") or {}).get("risk_score")) > 0.7
        ]
        medium_or_high = [
            seg for seg in aligned_segments
            if cls._clip_score((seg.get("audit_result") or {}).get("risk_score")) > 0.3
        ]
        high_duration = sum(float(seg.get("duration", 0.0) or 0.0) for seg in high_segments)
        high_duration_ratio = high_duration / total_duration if total_duration > 0 else 0.0

        if any(cls._is_direct_reject_segment(seg) for seg in aligned_segments):
            return "high"
        if not audit_complete:
            return "medium"
        if len(high_segments) >= 2 and high_duration_ratio >= 0.2:
            return "high"
        if len(high_segments) >= 3:
            return "high"
        if medium_or_high:
            return "medium"
        return "low"

    @staticmethod
    def normalize_risk_type(value):
        raw = str(value or "").strip()
        if not raw:
            return "normal"
        lowered = raw.lower()
        compact = lowered.replace(" ", "").replace("-", "").replace("_", "")
        aliases = {
            "none": "normal",
            "normal": "normal",
            "safe": "normal",
            "no": "normal",
            "无": "normal",
            "無": "normal",
            "violence": "violence",
            "violent": "violence",
            "暴力": "violence",
            "鏆村姏": "violence",
            "é†æ‘å§": "violence",
            "sexual": "sexual",
            "sex": "sexual",
            "porn": "sexual",
            "色情": "sexual",
            "adult": "sexual",
            "political": "political",
            "politics": "political",
            "politicalsensitive": "political",
            "政治": "political",
            "政治敏感": "political",
            "fraud": "fraud",
            "scam": "fraud",
            "诈骗": "fraud",
            "欺诈": "fraud",
            "引流诈骗": "fraud",
            "gambling": "gambling",
            "gamble": "gambling",
            "betting": "gambling",
            "赌博": "gambling",
            "博彩": "gambling",
            "drug": "drug",
            "drugs": "drug",
            "narcotics": "drug",
            "毒品": "drug",
            "minor": "minor_safety",
            "minorsafety": "minor_safety",
            "minor_safety": "minor_safety",
            "未成年": "minor_safety",
            "未成年人": "minor_safety",
            "selfharm": "self_harm",
            "self_harm": "self_harm",
            "suicide": "self_harm",
            "自残": "self_harm",
            "自杀": "self_harm",
            "privacy": "privacy",
            "doxxing": "privacy",
            "隐私": "privacy",
            "个人信息": "privacy",
            "hate": "hate_harassment",
            "harassment": "hate_harassment",
            "hateharassment": "hate_harassment",
            "hate_harassment": "hate_harassment",
            "辱骂": "hate_harassment",
            "歧视": "hate_harassment",
            "仇恨": "hate_harassment",
            "terror": "terror_extremism",
            "terrorism": "terror_extremism",
            "extremism": "terror_extremism",
            "terrorextremism": "terror_extremism",
            "terror_extremism": "terror_extremism",
            "恐怖": "terror_extremism",
            "极端": "terror_extremism",
            "vulgar": "vulgar",
            "softporn": "vulgar",
            "低俗": "vulgar",
            "擦边": "vulgar",
            "animalcruelty": "animal_cruelty",
            "animal_cruelty": "animal_cruelty",
            "虐待动物": "animal_cruelty",
            "虐猫": "animal_cruelty",
            "虐狗": "animal_cruelty",
            "copyright": "copyright",
            "piracy": "copyright",
            "版权": "copyright",
            "盗版": "copyright",
            "illegaltrade": "illegal_trade",
            "illegal_trade": "illegal_trade",
            "非法交易": "illegal_trade",
            "other": "other",
            "others": "other",
            "其他": "other",
            "audioevent": "audio_event",
            "audio_event": "audio_event",
            "auditincomplete": "audit_incomplete",
            "audit_incomplete": "audit_incomplete",
            "processingerror": "audit_incomplete",
            "parseerror": "audit_incomplete",
            "unknown": "audit_incomplete",
            "error": "audit_incomplete",
        }
        if raw in aliases:
            return aliases[raw]
        if lowered in aliases:
            return aliases[lowered]
        if compact in aliases:
            return aliases[compact]
        if "暴" in raw or "武器" in raw or "血" in raw:
            return "violence"
        if "色" in raw or "裸" in raw or "性" in raw:
            return "sexual"
        if "政治" in raw or "敏感" in raw:
            return "political"
        if "赌" in raw or "博彩" in raw:
            return "gambling"
        if "诈" in raw or "引流" in raw:
            return "fraud"
        if "毒" in raw or "大麻" in raw:
            return "drug"
        if "未成年" in raw or "幼" in raw:
            return "minor_safety"
        if "自杀" in raw or "自残" in raw or "轻生" in raw:
            return "self_harm"
        if "隐私" in raw or "个人信息" in raw or "开盒" in raw:
            return "privacy"
        if "辱骂" in raw or "歧视" in raw or "仇恨" in raw:
            return "hate_harassment"
        if "恐怖" in raw or "极端" in raw:
            return "terror_extremism"
        if "低俗" in raw or "擦边" in raw:
            return "vulgar"
        if "虐" in raw and "动物" in raw:
            return "animal_cruelty"
        if "版权" in raw or "盗版" in raw:
            return "copyright"
        if "非法" in raw or "违禁" in raw:
            return "illegal_trade"
        return "other"

    @staticmethod
    def _coerce_bool(value):
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            return value.strip().lower() in {"1", "true", "yes", "y", "on"}
        return bool(value)

    @classmethod
    def _normalize_audit_result(cls, result, segment=None):
        result = dict(result or {})
        try:
            risk_score = cls._clip_score(result.get("risk_score") or 0.0)
        except Exception:
            risk_score = 0.5
            result["audit_incomplete"] = True

        raw_risk_type = str(result.get("risk_type") or "").strip()
        risk_type = cls.normalize_risk_type(raw_risk_type)
        audit_incomplete = cls._coerce_bool(result.get("audit_incomplete")) or risk_type == "audit_incomplete"
        if audit_incomplete:
            result["is_risky"] = True
            result["risk_type"] = "audit_incomplete"
            result["risk_score"] = max(risk_score, 0.5)
            result["audit_incomplete"] = True
        else:
            is_risky = (
                cls._coerce_bool(result.get("is_risky"))
                or risk_score > 0.3
                or (risk_type in cls.ZERO_TOLERANCE_RISK_TYPES and risk_score > 0.2)
            )
            if risk_type == "normal" and is_risky:
                risk_type = "other"
            result["is_risky"] = is_risky
            result["risk_type"] = risk_type
            result["risk_score"] = risk_score

        if result.get("severity"):
            severity = str(result.get("severity")).strip().lower()
            if severity not in {"low", "medium", "high", "critical"}:
                severity = ""
            if severity:
                result["severity"] = severity
        if result.get("confidence") not in (None, ""):
            result["confidence"] = cls._clip_score(result.get("confidence"), default=0.0)
        if result.get("policy_action"):
            action = str(result.get("policy_action")).strip().lower()
            if action in {"pass", "monitor", "review", "reject"}:
                result["policy_action"] = action
        if result.get("evidence_modalities") is not None:
            result["evidence_modalities"] = cls._normalize_evidence_modalities(result.get("evidence_modalities"))

        reason = str(result.get("reason") or "").strip()
        if not reason:
            result["reason"] = "No reason provided by model."
        if segment is not None:
            result["segment_id"] = segment.get("segment_id")
            result["time_range"] = f"{segment.get('start')}s-{segment.get('end')}s"
        return result

    def _estimate_video_duration(self, video_path, frames_info, transcriptions, sound_events, item_meta=None):
        duration = 0.0
        video_duration = 0.0
        item_duration = self._read_meta_value(item_meta, "duration")
        try:
            if item_duration:
                duration = max(duration, float(item_duration))
        except Exception:
            pass
        if video_path:
            video_duration = float(self.get_video_duration(video_path) or 0.0)
            duration = max(duration, video_duration)
        if frames_info:
            duration = max(duration, max(float(frame.get("timestamp", 0.0)) for frame in frames_info))
        if transcriptions:
            duration = max(duration, max(float(item.get("end", 0.0)) for item in transcriptions))
        if sound_events:
            duration = max(duration, max(float(item.get("time", 0.0)) for item in sound_events) + 1.0)
        if video_duration > 0:
            duration = min(duration, video_duration)
        return max(duration, 0.0)

    @staticmethod
    def _sample_frames(frames, max_count=9):
        if len(frames) <= max_count:
            return list(frames)
        positions = np.linspace(0, len(frames) - 1, max_count)
        sampled = []
        seen = set()
        for position in positions:
            index = int(round(float(position)))
            if index not in seen:
                sampled.append(frames[index])
                seen.add(index)
        return sampled

    def _select_segment_frames(self, frames_info, start, end, max_count=9):
        if not frames_info:
            return [], None, None, 0
        midpoint = (float(start) + float(end)) / 2
        candidate_frames = [
            frame for frame in frames_info
            if float(start) - 0.5 <= float(frame.get("timestamp", 0.0)) <= float(end) + 0.5
        ]
        raw_count = len(candidate_frames)
        if not candidate_frames:
            candidate_frames = sorted(
                frames_info,
                key=lambda frame: abs(float(frame.get("timestamp", 0.0)) - midpoint),
            )[:max_count]
        else:
            candidate_frames = self._sample_frames(candidate_frames, max_count=max_count)
        best_frame = min(
            candidate_frames,
            key=lambda frame: abs(float(frame.get("timestamp", 0.0)) - midpoint),
        ) if candidate_frames else None
        time_diff = abs(float(best_frame.get("timestamp", 0.0)) - midpoint) if best_frame else None
        return candidate_frames, best_frame, time_diff, raw_count

    def _create_contact_sheet(self, segment, max_images=9):
        frames = [frame for frame in segment.get("segment_frames", []) if frame.get("path")]
        frames = frames[:max_images]
        if not frames:
            return None
        try:
            thumbs = []
            for frame in frames:
                image = Image.open(frame["path"]).convert("RGB")
                image.thumbnail((320, 180))
                canvas = Image.new("RGB", (320, 180), "white")
                left = (320 - image.width) // 2
                top = (180 - image.height) // 2
                canvas.paste(image, (left, top))
                thumbs.append(canvas)
                image.close()
            columns = 3
            rows = int(np.ceil(len(thumbs) / columns))
            sheet = Image.new("RGB", (columns * 320, rows * 180), "white")
            for index, thumb in enumerate(thumbs):
                sheet.paste(thumb, ((index % columns) * 320, (index // columns) * 180))
            base_path = Path(segment.get("best_frame_path") or frames[0]["path"])
            sheet_path = base_path.parent / f"segment_{segment.get('segment_id', 'unknown')}_contact.jpg"
            sheet.save(sheet_path, quality=90)
            return str(sheet_path)
        except Exception as exc:
            print(f"Failed to build contact sheet for segment {segment.get('segment_id')}: {exc}")
            return None

    def _build_audit_segment(
        self,
        segment_id,
        start,
        end,
        text,
        frames_info,
        metadata_summary,
        source_type,
        coverage_issues=None,
        long_text_context=False,
        source_transcript_text="",
        source_text_time_range="",
        text_scope="window",
    ):
        segment_frames, best_frame, time_diff, frame_count = self._select_segment_frames(frames_info, start, end)
        text_policy_matches = self._scan_segment_text_policy(text, metadata_summary)
        window_text = text or ""
        modalities = []
        if best_frame:
            modalities.append("visual")
        if window_text:
            modalities.append("speech_text" if source_type == "speech" else "text")
        if metadata_summary:
            modalities.append("metadata_text")
        if text_policy_matches:
            modalities.append("text_policy")
        segment = {
            "segment_id": segment_id,
            "start": round(float(start), 2),
            "end": round(float(max(end, start)), 2),
            "duration": round(float(max(end - start, 0.0)), 2),
            "text": window_text,
            "window_text": window_text,
            "source_transcript_text": source_transcript_text or "",
            "source_text_time_range": source_text_time_range or "",
            "text_scope": text_scope or "window",
            "source_type": source_type,
            "metadata_summary": metadata_summary or "",
            "text_policy_matches": text_policy_matches,
            "text_policy_summary": self._format_text_policy_summary(text_policy_matches),
            "long_text_context": bool(long_text_context),
            "best_frame": best_frame,
            "best_frame_time": best_frame.get("timestamp") if best_frame else None,
            "best_frame_path": best_frame.get("path") if best_frame else None,
            "segment_frames": segment_frames,
            "frame_count": frame_count,
            "time_diff": time_diff,
            "modalities": modalities,
            "coverage_issues": list(coverage_issues or []),
        }
        segment["contact_sheet_path"] = self._create_contact_sheet(segment)
        return segment

    @staticmethod
    def _split_windows(start, end, window_seconds=30.0):
        windows = []
        current = float(start)
        end = float(max(end, start))
        while current < end:
            window_end = min(end, current + window_seconds)
            windows.append((current, window_end))
            current = window_end
        if not windows and end >= start:
            windows.append((float(start), float(end)))
        return windows

    def build_audit_segments(
        self,
        transcriptions,
        frames_info,
        sound_events=None,
        video_path=None,
        video_meta=None,
        item_meta=None,
        coverage_issues=None,
    ):
        metadata_summary = self._build_metadata_summary(video_meta, item_meta)
        sound_events = sound_events or []
        duration = self._estimate_video_duration(video_path, frames_info, transcriptions, sound_events, item_meta)
        segments = []
        next_segment_id = 1

        def clamp_window(start, end):
            start = max(0.0, float(start))
            end = max(start, float(end))
            if duration > 0:
                start = min(start, duration)
                end = min(end, duration)
            return start, max(start, end)

        max_speech_window_seconds = 30.0

        for trans in sorted(transcriptions or [], key=lambda item: float(item.get("start", 0.0))):
            start, end = clamp_window(trans.get("start", 0.0), trans.get("end", 0.0))
            if duration > 0 and end <= start:
                continue
            text = str(trans.get("text") or "")
            word_timestamps = list(trans.get("words") or [])
            if not word_timestamps:
                for whisper_segment in trans.get("segments") or []:
                    if isinstance(whisper_segment, dict):
                        word_timestamps.extend(whisper_segment.get("words") or [])
            word_timestamps = [
                word for word in word_timestamps
                if isinstance(word, dict)
                and (
                    self._float_or_none(word.get("start")) is not None
                    or self._float_or_none(word.get("end")) is not None
                )
            ]
            speech_windows = self._split_windows(start, end, window_seconds=max_speech_window_seconds)
            is_long_text = len(speech_windows) > 1 or (end - start) > max_speech_window_seconds
            source_text_time_range = f"{start:.2f}s-{end:.2f}s" if is_long_text else ""
            for window_start, window_end in speech_windows:
                window_start, window_end = clamp_window(window_start, window_end)
                if duration > 0 and window_end <= window_start:
                    continue
                if word_timestamps:
                    window_words = self._words_for_window(word_timestamps, window_start, window_end)
                    window_text = self._join_word_text(window_words)
                    if not window_text and not is_long_text:
                        window_text = text
                    text_scope = "window"
                elif is_long_text:
                    window_text = ""
                    text_scope = "source_transcript_context"
                else:
                    window_text = text
                    text_scope = "window"
                segment = self._build_audit_segment(
                    next_segment_id,
                    window_start,
                    window_end,
                    window_text,
                    frames_info,
                    metadata_summary,
                    "speech",
                    coverage_issues,
                    long_text_context=is_long_text,
                    source_transcript_text=text if is_long_text else "",
                    source_text_time_range=source_text_time_range,
                    text_scope=text_scope,
                )
                segments.append(segment)
                next_segment_id += 1

        speech_ranges = []
        for item in transcriptions or []:
            start, end = clamp_window(item.get("start", 0.0), item.get("end", 0.0))
            if duration <= 0 or end > start:
                speech_ranges.append((start, end))
        visual_windows = []
        if not speech_ranges:
            visual_windows = self._split_windows(0.0, duration, window_seconds=30.0)
        else:
            cursor = 0.0
            for start, end in sorted(speech_ranges):
                if start - cursor > 2.0:
                    visual_windows.extend(self._split_windows(cursor, start, window_seconds=30.0))
                cursor = max(cursor, end)
            if duration - cursor > 2.0:
                visual_windows.extend(self._split_windows(cursor, duration, window_seconds=30.0))

        if not frames_info and not visual_windows:
            risk_sound_times = [float(event.get("time", 0.0)) for event in sound_events if event.get("is_risk")]
            visual_windows = [
                clamp_window(max(0.0, time - 1.0), time + 1.0)
                for time in risk_sound_times
            ]

        for start, end in visual_windows:
            start, end = clamp_window(start, end)
            if duration > 0 and end <= start:
                continue
            segment = self._build_audit_segment(
                next_segment_id,
                start,
                end,
                "",
                frames_info,
                metadata_summary,
                "visual_fallback",
                coverage_issues,
            )
            segments.append(segment)
            next_segment_id += 1

        if not segments and frames_info:
            first_time = float(frames_info[0].get("timestamp", 0.0))
            last_time = float(frames_info[-1].get("timestamp", first_time))
            start, end = clamp_window(first_time, max(last_time, first_time + 1.0))
            segment = self._build_audit_segment(
                next_segment_id,
                start,
                end,
                "",
                frames_info,
                metadata_summary,
                "visual_fallback",
                coverage_issues,
            )
            segments.append(segment)

        segments.sort(key=lambda item: (float(item.get("start", 0.0)), int(item.get("segment_id", 0))))
        for index, segment in enumerate(segments, start=1):
            old_id = segment.get("segment_id")
            segment["segment_id"] = index
            if segment.get("contact_sheet_path"):
                continue
            segment["contact_sheet_path"] = self._create_contact_sheet(segment)
            if old_id != index and segment.get("contact_sheet_path"):
                segment["contact_sheet_path"] = self._create_contact_sheet(segment)
        print(f"Built {len(segments)} audit segments for multimodal moderation.")
        return segments

    def align_text_with_frames(self, transcriptions, frames_info):
        """
        将转写的文本与视频帧对齐
        
        Args:
            transcriptions: Whisper转写结果（带时间戳）
            frames_info: 关键帧信息列表（带时间戳）
            
        Returns:
            list: 对齐后的结果，每个段落包含文本和对应的帧
        """
        print("开始音画对齐...")
        
        aligned_segments = []
        
        for trans in transcriptions:
            segment_start = trans['start']
            segment_end = trans['end']
            segment_mid = (segment_start + segment_end) / 2
            
            # 找到最接近该段落中间时间的帧
            best_frame = None
            min_time_diff = float('inf')
            
            for frame in frames_info:
                time_diff = abs(frame['timestamp'] - segment_mid)
                if time_diff < min_time_diff:
                    min_time_diff = time_diff
                    best_frame = frame
            
            # 如果找到的帧时间差太大（超过2秒），可能不匹配
            if min_time_diff > 2.0:
                print(f"⚠️ 段落 {trans['segment_id']} 时间 {segment_mid:.2f}s 附近无匹配帧，最近帧在 {best_frame['timestamp']:.2f}s")
            
            # 同时找出该时间段内的所有帧（用于详细分析）
            segment_frames = []
            for frame in frames_info:
                if segment_start - 0.5 <= frame['timestamp'] <= segment_end + 0.5:
                    segment_frames.append(frame)
            
            # 构建对齐结果
            aligned = trans.copy()
            aligned.update({
                "best_frame": best_frame,
                "best_frame_time": best_frame['timestamp'] if best_frame else None,
                "best_frame_path": best_frame['path'] if best_frame else None,
                "segment_frames": segment_frames[:5],  # 最多保留5帧
                "frame_count": len(segment_frames),
                "time_diff": min_time_diff if best_frame else None
            })
            
            aligned_segments.append(aligned)
            
            # 打印对齐信息
            frame_marker = "✅" if min_time_diff < 1.0 else "⚠️" if min_time_diff < 2.0 else "❌"
            print(f"  {frame_marker} 段落 {trans['segment_id']}: 时间 {segment_start:.2f}s-{segment_end:.2f}s → 帧时间 {best_frame['timestamp']:.2f}s (偏差 {min_time_diff:.2f}s)")
        
        print(f"✅ 音画对齐完成，共处理 {len(aligned_segments)} 个段落")
        return aligned_segments
    
    def print_aligned_segments(self, aligned_segments):
        """
        打印对齐后的段落信息
        
        Args:
            aligned_segments: 对齐后的段落列表
        """
        if not aligned_segments:
            print("没有对齐结果")
            return
        
        print("\n🎯 音画对齐结果:")
        print("=" * 80)
        
        for i, seg in enumerate(aligned_segments):
            print(f"[段落 {seg['segment_id']}] {seg['start']:.2f}s - {seg['end']:.2f}s")
            print(f"文本: {seg['text'][:80]}...")
            
            if seg['best_frame']:
                marker = "✓" if seg['time_diff'] < 1.0 else "⚠" if seg['time_diff'] < 2.0 else "✗"
                print(f"   {marker} 匹配帧: 时间 {seg['best_frame_time']:.2f}s (偏差 {seg['time_diff']:.2f}s)")
                print(f"     路径: {seg['best_frame_path']}")
            else:
                print("   ✗ 无匹配帧")
            
            print(f"   该时段共有 {seg['frame_count']} 帧")
            print("-" * 40)
        
        print(f"总计: {len(aligned_segments)} 个段落完成对齐")
        print("=" * 80)

    def align_sound_with_segments(self, aligned_segments, sound_events):
        """
        将声音事件与对齐后的文本段落对齐
        
        Args:
            aligned_segments: 音画对齐后的段落列表
            sound_events: YAMNet声音事件列表
            
        Returns:
            list: 每个段落都包含该时间段内的声音事件
        """
        print("开始声音事件对齐...")
        
        for segment in aligned_segments:
            segment_start = segment['start']
            segment_end = segment['end']
            
            # 找出这个时间段内的所有声音事件
            segment_sounds = []
            risk_sounds = []
            
            for sound in sound_events:
                if segment_start <= sound['time'] <= segment_end:
                    segment_sounds.append(sound)
                    if sound.get('is_risk', False):
                        risk_sounds.append(sound)
            
            # 添加到段落中
            segment['sound_events'] = segment_sounds
            segment['risk_sounds'] = risk_sounds
            segment['has_risk_sound'] = len(risk_sounds) > 0
            segment_modalities = segment.setdefault('modalities', [])
            if segment_sounds and 'audio_event' not in segment_modalities:
                segment_modalities.append('audio_event')
            
            # 生成声音摘要（用于提示词）
            if risk_sounds:
                sound_summary = "，".join([
                    f"{s['class']}(置信度{s['confidence']:.2f})" 
                    for s in risk_sounds[:3]
                ])
                segment['sound_summary'] = f"⚠️ 检测到风险声音：{sound_summary}"
            else:
                # 如果没有风险声音，也记录正常声音
                if segment_sounds:
                    normal_sounds = list(set([s['class'] for s in segment_sounds]))[:3]
                    segment['sound_summary'] = f"背景声音：{', '.join(normal_sounds)}"
                else:
                    segment['sound_summary'] = "背景声音：无特殊声音"
            
            # 打印对齐信息
            sound_marker = "RISK" if risk_sounds else "OK"
            print(f"  {sound_marker} 段落 {segment['segment_id']}: {len(segment_sounds)}个声音事件, {len(risk_sounds)}个风险声音")
        
        print("Sound event alignment complete")
        return aligned_segments
    
    def build_audit_prompt(self, segment):
        """
        构建多模态审核的提示词
        
        Args:
            segment: 包含文本、帧、声音信息的段落
            
        Returns:
            str: 完整的提示词
        """
        window_text = segment.get("window_text")
        if window_text is None:
            window_text = segment.get("text") or ""
        text_content = self._truncate_text(window_text or "", limit=1200)
        source_transcript_text = self._truncate_text(segment.get("source_transcript_text") or "", limit=1600)
        source_text_time_range = segment.get("source_text_time_range") or "无"
        text_scope = segment.get("text_scope") or "window"
        metadata_summary = segment.get('metadata_summary') or "无"
        sound_summary = segment.get('sound_summary') or "背景声音：无特殊声音"
        source_type = segment.get('source_type') or "unknown"
        frame_count = len(segment.get('segment_frames') or [])
        text_policy_summary = segment.get("text_policy_summary") or "无文本规则命中"
        if segment.get("long_text_context"):
            long_text_note = (
                "该语音转写来自长语音段，已按30秒视觉窗口拆分；【文字内容】仅表示当前窗口内按词级时间戳命中的文字。"
                "【整段语音上下文】不是当前窗口专属证据，只能辅助理解，不能单独用于判定当前窗口违规。"
            )
        else:
            long_text_note = "否"
        source_transcript_block = (
            f'text_scope={text_scope}; source_time_range={source_text_time_range}; "{source_transcript_text}"'
            if source_transcript_text
            else "无"
        )
        risk_type_list = "/".join([
            "violence",
            "sexual",
            "political",
            "fraud",
            "gambling",
            "drug",
            "minor_safety",
            "self_harm",
            "privacy",
            "hate_harassment",
            "terror_extremism",
            "vulgar",
            "animal_cruelty",
            "copyright",
            "illegal_trade",
            "other",
            "normal",
        ])
        prompt = f"""请审核这段视频内容，结合画面、文字、声音三方面信息综合判断是否存在违规内容。

        【时间范围】
        {segment['start']}秒 - {segment['end']}秒

        【画面信息】
        （见附图中的视频帧，展示该时间段内的关键画面）
        source_type={source_type}; representative_frame_count={frame_count}

        【文字内容】
        "{text_content}"

        【整段语音上下文（非当前窗口专属）】
        {source_transcript_block}

        【投稿元数据/标题简介标签文件名】
        "{metadata_summary}"

        【文本规则预检】
        {text_policy_summary}

        【背景声音】
        {sound_summary}

        【长文本上下文】
        {long_text_note}

        【审核要求】
        请基于以上所有信息（画面+文字+声音）综合判断，不要仅依赖单一模态。
        如果【整段语音上下文（非当前窗口专属）】与【文字内容】不一致，必须以当前时间窗口的【文字内容】、画面、声音为准；整段语音上下文只能辅助理解，不得单独作为当前窗口违规证据。
        Please also inspect visible text in the image, including subtitles, slogans, cover text, posters, UI text, and watermarks. Treat the title, introduction, tags, cover information, and file name as moderation text too. If there is no speech transcript, still judge the visual content independently.

        需要检测的违规类型包括：

        1. 暴力内容：
        - 画面：打架、流血、武器、暴力场景
        - 文字：暴力威胁、血腥描述、伤害言论
        - 声音：枪声、爆炸声、打斗声、尖叫声、哭声
        - 语境：影视/游戏/动画中的武器、战斗、爆炸，如果没有真实伤害、血腥、教程、威胁、教唆，通常不要直接拦截，最高建议人工复核

        2. 色情内容：
        - 画面：裸露、性暗示、不雅动作
        - 文字：色情描述、露骨言论、性暗示
        - 声音：暧昧声音、喘息声

        3. 政治敏感：
        - 画面：敏感标志、敏感人物、敏感场景
        - 文字：政治敏感言论、违规表述
        - 声音：敏感口号、政治性声音

        4. 高频平台违规：
        - fraud：诈骗、刷单、返利、贷款、投资群、加联系方式导流
        - gambling：赌博、博彩、下注、盘口、赔率、赌场
        - drug：毒品交易、吸毒展示、制毒或购买暗示
        - minor_safety：未成年人性风险、诱导、隐私暴露
        - self_harm：自残自杀表达、教程、鼓励轻生
        - privacy：身份证、手机号、住址、开盒、人肉、隐私泄露
        - hate_harassment：辱骂、歧视、仇恨、持续骚扰
        - terror_extremism：恐怖极端组织、口号、暴恐教程
        - vulgar：低俗擦边、裸聊、大尺度引流
        - animal_cruelty：虐待动物
        - copyright：盗版资源、未授权搬运、全集下载
        - illegal_trade：枪支、管制刀具、假证、违禁品交易

        【综合评分规则】
        - policy_action=reject：明确命中平台不可放行场景，risk_score 通常 >=0.78
        - policy_action=review：上下文不足或影视/游戏暴力等边界场景，risk_score 通常 0.31-0.70
        - policy_action=pass：未发现违规，risk_score <=0.30
        - 单独音频枪声/爆炸/尖叫等，如果画面和文字不支持真实风险，应建议 review 而不是 reject
        - 请给出 content_context：game/film/animation/news/real/unknown
        - 请给出 evidence_modalities：visual/speech_text/metadata_text/audio_event/text_policy 中命中的证据来源

        【输出格式】
        请严格按照以下JSON格式输出审核结果，不要有其他文字：

        {{
            "is_risky": true/false,
            "risk_type": "{risk_type_list}",
            "risk_score": 0.85,
            "reason": "审核理由，说明画面、文字、声音各自的情况及综合判断依据",
            "risk_subtype": "可选：更细场景，如 fictional_weapon_display/scam_or_diversion",
            "severity": "low/medium/high/critical",
            "confidence": 0.0,
            "content_context": "game/film/animation/news/real/unknown",
            "policy_action": "pass/review/reject",
            "evidence_modalities": ["visual", "speech_text"]
        }}

        注意：
        - risk_type 必须只使用上面列出的英文稳定标签；没有风险时使用 normal
        - risk_score 范围0-1，**分数越高表示风险越高**
        - 高风险 >0.7 建议拦截
        - 中风险 0.3-0.7 建议人工复核
        - 低风险 <0.3 可通过
        """
            
        return prompt
    
    def audit_with_qwen(self, segment):
        """
        使用Qwen模型审核单个段落
        
        Args:
            segment: 包含文本、帧路径、声音信息的段落
            
        Returns:
            dict: 审核结果
        """
        image_path = segment.get('contact_sheet_path') or segment.get('best_frame_path')
        if not image_path or not Path(image_path).exists():
            segment_frames = segment.get('segment_frames') or []
            if segment_frames:
                image_path = segment_frames[0].get('path')
        prompt = self.build_audit_prompt(segment)
        image_snapshot_path = self._copy_trace_image(segment, image_path)
        message_payload = self._build_trace_message_payload(segment, prompt, image_path, image_snapshot_path)
        generation_params = {
            "max_new_tokens": 384,
            "do_sample": False,
            "temperature": 0.1,
        }
        if not image_path or not Path(image_path).exists():
            result = {
                "is_risky": True,
                "risk_type": "audit_incomplete",
                "risk_score": 0.5,
                "reason": "No usable visual frame was available; fallback to manual review.",
                "segment_id": segment.get('segment_id'),
                "audit_incomplete": True,
            }
            trace_file = self._write_model_input_trace(
                segment,
                prompt=prompt,
                message_payload=message_payload,
                image_path=image_path or "",
                image_snapshot_path=image_snapshot_path,
                generation_params=generation_params,
                parsed_result=result,
                error=result["reason"],
            )
            if trace_file:
                result["model_input_trace_file"] = trace_file
            return result

        if self.qwen_model is None or self.qwen_processor is None:
            print("Qwen model is not loaded")
            result = {
                "is_risky": True,
                "risk_type": "audit_incomplete",
                "risk_score": 0.5,
                "reason": "Qwen model is not loaded; fallback to manual review.",
                "segment_id": segment.get('segment_id'),
                "audit_incomplete": True,
            }
            trace_file = self._write_model_input_trace(
                segment,
                prompt=prompt,
                message_payload=message_payload,
                image_path=image_path,
                image_snapshot_path=image_snapshot_path,
                generation_params=generation_params,
                parsed_result=result,
                error=result["reason"],
            )
            if trace_file:
                result["model_input_trace_file"] = trace_file
            return result
        
        chat_template_text = ""
        raw_model_response = None
        parsed_result = None
        try:
            from PIL import Image
            import torch
            
            print(f"审核段落 {segment['segment_id']}: {segment['start']:.2f}s - {segment['end']:.2f}s")
            
            # 1. 准备图片
            image = Image.open(image_path)
            
            # 3. 准备Qwen输入
            messages = [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "image": image,
                        },
                        {
                            "type": "text",
                            "text": prompt,
                        },
                    ],
                }
            ]
            
            # 4. 处理输入
            text = self.qwen_processor.apply_chat_template(
                messages, 
                tokenize=False, 
                add_generation_prompt=True
            )
            chat_template_text = text
            
            inputs = self.qwen_processor(
                text=[text], 
                images=[image], 
                padding=True, 
                return_tensors="pt"
            )
            try:
                image.close()
            except Exception:
                pass
            
            # 5. 移到设备
            qwen_device = self.get_qwen_runtime_device()
            inputs = {k: v.to(qwen_device) if hasattr(v, "to") else v for k, v in inputs.items()}
            
            # 6. 生成
            with torch.no_grad():
                generated_ids = self.qwen_model.generate(
                    **inputs,
                    **generation_params,
                )
            
            # 7. 解码
            generated_ids_trimmed = [
                out_ids[len(in_ids):] for in_ids, out_ids in zip(inputs['input_ids'], generated_ids)
            ]
            response = self.qwen_processor.batch_decode(
                generated_ids_trimmed, 
                skip_special_tokens=True, 
                clean_up_tokenization_spaces=False
            )[0]
            raw_model_response = response
            
            # 8. 解析JSON结果
            import json
            import re
            
            # 尝试从response中提取JSON
            json_match = re.search(r'\{.*\}', response, re.DOTALL)
            if json_match:
                try:
                    parsed_result = json.loads(json_match.group())
                    result = parsed_result
                except:
                    # 如果解析失败，使用默认值
                    result = {
                        "is_risky": True,
                        "risk_type": "audit_incomplete",
                        "risk_score": 0.5,
                        "reason": f"Qwen JSON parse failed: {response[:200]}",
                        "audit_incomplete": True,
                    }
                    parsed_result = result
            else:
                result = {
                    "is_risky": True,
                    "risk_type": "audit_incomplete",
                    "risk_score": 0.5,
                    "reason": f"Qwen JSON parse failed: {response[:200]}",
                    "audit_incomplete": True,
                }
                parsed_result = result
            
            # 9. 添加元数据
            result = self._normalize_audit_result(result, segment)
            trace_file = self._write_model_input_trace(
                segment,
                prompt=prompt,
                chat_template_text=chat_template_text,
                message_payload=message_payload,
                image_path=image_path,
                image_snapshot_path=image_snapshot_path,
                generation_params=generation_params,
                raw_model_response=raw_model_response,
                parsed_result=parsed_result,
            )
            if trace_file:
                result["model_input_trace_file"] = trace_file
            
            # 10. 打印结果
            risk_level = "高风险" if result['risk_score'] > 0.7 else "中风险" if result['risk_score'] > 0.3 else "低风险"
            print(f"  {risk_level}: 得分 {result['risk_score']:.2f}, 类型: {result['risk_type']}")
            print(f"  理由: {result['reason'][:100]}...")
            
            return result
            
        except Exception as e:
            print(f"审核失败: {e}")
            import traceback
            traceback.print_exc()
            result = {
                "is_risky": True,
                "risk_type": "audit_incomplete",
                "risk_score": 0.5,
                "reason": f"Qwen audit error: {str(e)}",
                "segment_id": segment.get('segment_id'),
                "audit_incomplete": True,
            }
            trace_file = self._write_model_input_trace(
                segment,
                prompt=prompt,
                chat_template_text=chat_template_text,
                message_payload=message_payload,
                image_path=image_path,
                image_snapshot_path=image_snapshot_path,
                generation_params=generation_params,
                raw_model_response=raw_model_response,
                parsed_result=parsed_result or result,
                error=str(e),
            )
            if trace_file:
                result["model_input_trace_file"] = trace_file
            return result
        
    @staticmethod
    def _apply_audio_risk_fusion(segment, result):
        if not segment.get('has_risk_sound'):
            return result
        if result.get("audio_risk_fused"):
            return VideoModerationService._normalize_audit_result(result, segment)
        risk_sounds = segment.get('risk_sounds') or []
        sound_summary = ", ".join(
            f"{sound.get('class')}({float(sound.get('confidence', 0.0)):.2f})"
            for sound in risk_sounds[:3]
        ) or "risk sound"
        result = VideoModerationService._normalize_audit_result(result, segment)
        current_score = float(result.get("risk_score") or 0.0)
        if current_score < 0.5:
            result["risk_score"] = 0.5
        if VideoModerationService.normalize_risk_type(result.get("risk_type")) == "normal":
            result["risk_type"] = "audio_event"
            result["risk_subtype"] = (risk_sounds[0].get("risk_category") if risk_sounds else "risk_sound")
        if current_score <= 0.7 and str(result.get("policy_action") or "").lower() != "reject":
            result["policy_action"] = "review"
            result["severity"] = result.get("severity") or "medium"
        result["evidence_modalities"] = VideoModerationService._segment_evidence_modalities(segment, result)
        if not result.get("is_risky") and float(result.get("risk_score") or 0.0) > 0.3:
            result["is_risky"] = True
        reason = str(result.get("reason") or "")
        audio_reason = f"Risk audio event detected by YAMNet: {sound_summary}; fallback to manual review."
        result["reason"] = f"{reason} {audio_reason}".strip()
        result["audio_risk_fused"] = True
        return result

    def batch_audit(self, aligned_segments, max_segments=None):
        """
        批量审核所有段落
        
        Args:
            aligned_segments: 对齐后的段落列表（已包含声音事件）
            max_segments: 最大审核段落数（用于测试）
            
        Returns:
            list: 所有段落的审核结果
        """
        print("\n" + "="*60)
        print("开始多模态审核...")
        print("="*60)
        
        audit_results = []
        
        # 限制数量用于测试
        segments_to_audit = aligned_segments
        if max_segments:
            segments_to_audit = aligned_segments[:max_segments]
            print(f"⚠️ 测试模式：只审核前 {max_segments} 个段落")
        
        for i, segment in enumerate(segments_to_audit):
            print(f"\n--- 审核进度 {i+1}/{len(segments_to_audit)} ---")
            
            # 审核当前段落
            result = self.audit_with_qwen(segment)
            result = self._normalize_audit_result(result, segment)
            result = self._apply_text_policy_fusion(segment, result)
            result = self._apply_audio_risk_fusion(segment, result)
            result = self._apply_business_calibration(segment, result)
            self.update_model_input_trace_final_result(segment, result)
            audit_results.append(result)
            
            # 将结果添加到segment中
            segment['audit_result'] = result
        
        return audit_results

    def generate_audit_report(self, aligned_segments, video_path, coverage_issues=None, modalities_checked=None):
        """
        生成完整的审核报告
        
        Args:
            aligned_segments: 对齐后的段落列表
            video_path: 视频文件路径
            
        Returns:
            dict: 审核报告
        """
        print("\n" + "="*60)
        print("生成审核报告...")
        print("="*60)
        
        for seg in aligned_segments:
            if seg.get('audit_result'):
                result = self._normalize_audit_result(seg['audit_result'], seg)
                result = self._apply_text_policy_fusion(seg, result)
                result = self._apply_audio_risk_fusion(seg, result)
                result = self._apply_business_calibration(seg, result)
                self.update_model_input_trace_final_result(seg, result)
                seg['audit_result'] = result

        # 统计所有结果
        total_segments = len(aligned_segments)
        risky_segments = [s for s in aligned_segments if s.get('audit_result', {}).get('is_risky', False)]
        high_risk = [s for s in aligned_segments if s.get('audit_result', {}).get('risk_score', 0) > 0.7]
        medium_risk = [s for s in aligned_segments if 0.3 < s.get('audit_result', {}).get('risk_score', 0) <= 0.7]
        low_risk = [s for s in aligned_segments if s.get('audit_result', {}).get('risk_score', 0) <= 0.3]
        
        # 按风险类型统计
        risk_types = {}
        for seg in risky_segments:
            rtype = self.normalize_risk_type(seg.get('audit_result', {}).get('risk_type', 'audit_incomplete'))
            risk_types[rtype] = risk_types.get(rtype, 0) + 1
        coverage_issues = list(dict.fromkeys(str(issue) for issue in (coverage_issues or []) if issue))
        segment_incomplete = [
            seg for seg in aligned_segments
            if seg.get('audit_result', {}).get('audit_incomplete')
        ]
        if segment_incomplete and "qwen_audit_incomplete" not in coverage_issues:
            coverage_issues.append("qwen_audit_incomplete")
        if total_segments == 0 and "no_audit_segments" not in coverage_issues:
            coverage_issues.append("no_audit_segments")
        collected_modalities = set(modalities_checked or [])
        for seg in aligned_segments:
            collected_modalities.update(seg.get("modalities") or [])
        audit_complete = len(coverage_issues) == 0
        
        # 构建报告
        report = {
            "video_path": str(video_path),
            "total_segments": total_segments,
            "risky_segments": len(risky_segments),
            "risk_rate": len(risky_segments) / total_segments if total_segments > 0 else 0,
            "high_risk_count": len(high_risk),
            "medium_risk_count": len(medium_risk),
            "low_risk_count": len(low_risk),
            "risk_type_distribution": risk_types,
            "modalities_checked": sorted(collected_modalities),
            "coverage_issues": coverage_issues,
            "audit_complete": audit_complete,
            "segments": []
        }
        trace_files = [
            str((seg.get("audit_result") or {}).get("model_input_trace_file") or seg.get("model_input_trace_file"))
            for seg in aligned_segments
            if ((seg.get("audit_result") or {}).get("model_input_trace_file") or seg.get("model_input_trace_file"))
        ]
        if trace_files:
            report["model_input_trace_dir"] = str(self._trace_session_dir) if self._trace_session_dir else str(Path(trace_files[0]).parent)
            report["model_input_trace_files"] = trace_files
        
        # 添加每个段落的简要结果
        for seg in aligned_segments:
            if 'audit_result' in seg:
                report["segments"].append({
                    "segment_id": seg['segment_id'],
                    "time": f"{seg['start']:.2f}s-{seg['end']:.2f}s",
                    "text_preview": str(seg.get('text') or '')[:100] + "...",
                    "has_risk_sound": seg.get('has_risk_sound', False),
                    "audit_result": seg['audit_result']
                })
        
        # 决策建议
        decision_level = self._decide_report_level(aligned_segments, audit_complete)
        if decision_level == "high":
            report["decision"] = "高风险 - 建议拦截"
            report["decision_level"] = "high"
        elif decision_level == "medium":
            report["decision"] = "审核不充分 - 建议人工复核"
            if report.get("audit_complete", True):
                report["decision"] = "中风险 - 建议人工复核"
            report["decision_level"] = "medium"
        else:
            report["decision"] = "低风险 - 建议通过"
            report["decision_level"] = "low"
        
        # 保存报告
        import json
        from datetime import datetime
        
        report_path = self.audit_results_dir / f"audit_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        print(f"Audit report saved to: {report_path}")
        
        return report
    
    def print_audit_summary(self, report):
        """
        打印审核总结
        
        Args:
            report: 审核报告
        """
        print("\n" + "="*60)
        print("📊 审核结果总结")
        print("="*60)
        
        print(f"视频: {report['video_path']}")
        print(f"总段落数: {report['total_segments']}")
        print(f"风险段落: {report['risky_segments']} ({report['risk_rate']*100:.1f}%)")
        print("-" * 40)
        print(f"🔴 高风险: {report['high_risk_count']} 段")
        print(f"🟡 中风险: {report['medium_risk_count']} 段")
        print(f"🟢 低风险: {report['low_risk_count']} 段")
        
        if report['risk_type_distribution']:
            print("\n风险类型分布:")
            for rtype, count in report['risk_type_distribution'].items():
                print(f"  {rtype}: {count} 段")
        
        print("-" * 40)
        print(f"🎯 最终决策: {report['decision']}")
        
        # 列出高风险段落
        if report['high_risk_count'] > 0:
            print("\n🔴 高风险段落详情:")
            for seg in report['segments']:
                if seg['audit_result']['risk_score'] > 0.7:
                    print(f"  [{seg['time']}] 得分: {seg['audit_result']['risk_score']:.2f}")
                    print(f"  类型: {seg['audit_result']['risk_type']}")
                    print(f"  声音风险: {'⚠️ 有' if seg['has_risk_sound'] else '无'}")
                    print(f"  文本: {seg['text_preview']}")
                    print()
        
        print("="*60)

# 使用示例
if __name__ == "__main__":
    # 创建服务实例
    service = VideoModerationService(model_dir="./models", device='cpu')
    
    # 加载所有模型
    service.load_all_models()
    
    # 查看模型信息
    info = service.get_model_info()
    print("\n模型加载状态:")
    for model_name, status in info.items():
        print(f"  {model_name}: {status}")

    try:
        video_file = "打瓦.mp4"
        audio_file = service.extract_audio_from_video(video_file)
        
        # 获取音频时长
        duration = service.get_audio_duration(audio_file)
        print(f"音频时长: {duration}秒")
    
    except Exception as e:
        print(f"处理失败: {e}")

    
    # VAD检测
    segments = service.detect_voice_segments(audio_file)
    service.print_voice_segments(segments, duration)

    # YAMNet声学检测
    print("\n" + "="*60)
    sound_events = service.detect_sound_events(
        audio_file, 
        top_k=5, 
        confidence_threshold=0.3
    )
    service.print_sound_events(sound_events, top_n=15)

    #whisper转写
    print("\n" + "="*60)
    transcriptions = service.transcribe_audio_segments(
        audio_file, 
        segments, 
        language=None
    )
    service.print_transcriptions(transcriptions)

    # 提取关键帧
    print("\n" + "="*60)
    frames_info = service.extract_keyframes(video_file, extraction_rate=1)

    # 音画对齐
    print("\n" + "="*60)
    aligned_segments = service.align_text_with_frames(transcriptions, frames_info)
    service.print_aligned_segments(aligned_segments)

    # 声音事件对齐
    print("\n" + "="*60)
    aligned_segments = service.align_sound_with_segments(aligned_segments, sound_events)

    # 多模态审核（新增）
    audit_results = service.batch_audit(aligned_segments, max_segments=None)  # None表示审核全部

    # 生成审核报告（新增）
    report = service.generate_audit_report(aligned_segments, video_file)

    # 打印总结（新增）
    service.print_audit_summary(report)