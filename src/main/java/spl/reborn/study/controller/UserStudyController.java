// spl.reborn.study.controller.UserStudyController
package spl.reborn.study.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spl.reborn.study.service.UserStudyService;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies")
public class UserStudyController {

    private final UserStudyService userStudyService;

    @PostMapping
    public ResponseEntity<Long> save(@RequestParam long userId,
                                     @RequestParam String contentTitle,
                                     @RequestParam LocalDate studyDate) {
        Long id = userStudyService.saveStudy(userId, contentTitle, studyDate);
        return ResponseEntity.ok(id);
    }
}
