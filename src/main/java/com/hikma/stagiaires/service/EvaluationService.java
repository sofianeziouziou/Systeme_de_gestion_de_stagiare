package com.hikma.stagiaires.service;

import com.hikma.stagiaires.dto.evaluation.EvaluationDTOs.*;
import com.hikma.stagiaires.model.*;
import com.hikma.stagiaires.repository.EvaluationRepository;
import com.hikma.stagiaires.repository.UserRepository;
import com.hikma.stagiaires.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final StagiaireService stagiaireService;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public EvaluationResponse create(CreateRequest req, String tuteurId) {
        Evaluation eval = Evaluation.builder()
                .stagiaireId(req.getStagiaireId())
                .tuteurId(tuteurId)
                .projetId(req.getProjetId())
                .qualiteTechnique(req.getQualiteTechnique())
                .respectDelais(req.getRespectDelais())
                .communication(req.getCommunication())
                .espritEquipe(req.getEspritEquipe())
                .commentaire(req.getCommentaire())
                .status(req.getStatus())
                .build();

        eval.calculateScore();
        Evaluation saved = evaluationRepository.save(eval);

        // Si soumise, notifier RH et mettre à jour le score du stagiaire
        if (EvaluationStatus.SOUMISE.equals(saved.getStatus())) {
            stagiaireService.updateScore(saved.getStagiaireId(), saved.getScoreGlobal(), saved.getId());
            notificationService.notifyEvaluationSoumise(saved);
        }

        auditLogService.log(tuteurId, "CREATE", "EVALUATION", saved.getId(), null);
        return toResponse(saved);
    }

    public EvaluationResponse update(String id, UpdateRequest req, String userId) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evaluation introuvable : " + id));

        if (req.getQualiteTechnique() != null) eval.setQualiteTechnique(req.getQualiteTechnique());
        if (req.getRespectDelais() != null) eval.setRespectDelais(req.getRespectDelais());
        if (req.getCommunication() != null) eval.setCommunication(req.getCommunication());
        if (req.getEspritEquipe() != null) eval.setEspritEquipe(req.getEspritEquipe());
        if (req.getCommentaire() != null) eval.setCommentaire(req.getCommentaire());
        if (req.getStatus() != null) eval.setStatus(req.getStatus());

        eval.calculateScore();
        Evaluation saved = evaluationRepository.save(eval);

        if (EvaluationStatus.SOUMISE.equals(saved.getStatus())) {
            stagiaireService.updateScore(saved.getStagiaireId(), saved.getScoreGlobal(), saved.getId());
        }

        return toResponse(saved);
    }

    public EvaluationResponse validate(String id, String rhUserId) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evaluation introuvable : " + id));

        eval.setStatus(EvaluationStatus.VALIDEE);
        Evaluation saved = evaluationRepository.save(eval);
        notificationService.notifyEvaluationValidee(saved);
        auditLogService.log(rhUserId, "VALIDATE", "EVALUATION", id, null);
        return toResponse(saved);
    }

    public List<EvaluationResponse> getByStagiaire(String stagiaireId) {
        return evaluationRepository.findByStagiaireId(stagiaireId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<EvaluationResponse> getByTuteur(String tuteurId) {
        return evaluationRepository.findByTuteurId(tuteurId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ScoreBreakdown getScoreBreakdown(String stagiaireId) {
        List<Evaluation> evals = evaluationRepository.findByStagiaireIdAndStatus(
                stagiaireId, EvaluationStatus.VALIDEE);

        if (evals.isEmpty()) return new ScoreBreakdown();

        double avgTech = evals.stream().mapToDouble(Evaluation::getQualiteTechnique).average().orElse(0);
        double avgDelais = evals.stream().mapToDouble(Evaluation::getRespectDelais).average().orElse(0);
        double avgComm = evals.stream().mapToDouble(Evaluation::getCommunication).average().orElse(0);
        double avgEquipe = evals.stream().mapToDouble(Evaluation::getEspritEquipe).average().orElse(0);
        double globalScore = (avgTech * 0.40) + (avgDelais * 0.20) + (avgComm * 0.20) + (avgEquipe * 0.20);

        ScoreBreakdown bd = new ScoreBreakdown();
        bd.setQualiteTechnique(avgTech);
        bd.setRespectDelais(avgDelais);
        bd.setCommunication(avgComm);
        bd.setEspritEquipe(avgEquipe);
        bd.setScoreGlobal(globalScore);
        bd.setBadge(calculateBadgeLabel(globalScore));
        return bd;
    }

    private String calculateBadgeLabel(double score) {
        if (score >= 90) return "EXCELLENCE";
        if (score >= 75) return "TRES_BIEN";
        if (score >= 60) return "BIEN";
        return "A_SURVEILLER";
    }

    private EvaluationResponse toResponse(Evaluation e) {
        EvaluationResponse r = new EvaluationResponse();
        r.setId(e.getId());
        r.setStagiaireId(e.getStagiaireId());
        r.setTuteurId(e.getTuteurId());
        r.setProjetId(e.getProjetId());
        r.setQualiteTechnique(e.getQualiteTechnique());
        r.setRespectDelais(e.getRespectDelais());
        r.setCommunication(e.getCommunication());
        r.setEspritEquipe(e.getEspritEquipe());
        r.setScoreGlobal(e.getScoreGlobal());
        r.setCommentaire(e.getCommentaire());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());

        userRepository.findById(e.getTuteurId()).ifPresent(u ->
                r.setTuteurFullName(u.getFirstName() + " " + u.getLastName()));
        if (e.getProjetId() != null) {
            projetRepository.findById(e.getProjetId()).ifPresent(p ->
                    r.setProjetTitle(p.getTitle()));
        }
        return r;
    }
}